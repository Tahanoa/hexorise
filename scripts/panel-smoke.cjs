// Browser smoke check against the real application and PostgreSQL used by CI.
const { chromium } = require('playwright');
const { spawn } = require('node:child_process');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const base = 'http://127.0.0.1:8080';
const username = 'panel-check', password = 'panel-check-password-long';
const log = fs.openSync('/tmp/hexorise-panel-server.log', 'w');
const app = spawn('java', ['-jar', 'target/hexorise-0.1.0-SNAPSHOT.jar'], {
  env: { ...process.env, ADMIN_USERNAME: username, ADMIN_PASSWORD: password, HIGHRISE_ENABLED: 'false', HIGHRISE_ROOM_ID: 'panel-test-room' },
  stdio: ['ignore', log, log]
});
const pause = ms => new Promise(resolve => setTimeout(resolve, ms));
(async () => {
  let ready = false, browser, page;
  try {
    for (let attempt = 0; attempt < 60; attempt++) {
      if (app.exitCode !== null) throw new Error('Application exited before becoming ready.');
      try { if ((await fetch(base + '/actuator/health')).ok) { ready = true; break; } } catch {}
      await pause(500);
    }
    assert(ready, 'Application readiness timed out.');
    browser = await chromium.launch({ headless: true });
    const context = await browser.newContext({ httpCredentials: { username, password }, viewport: { width: 1440, height: 1000 } });
    page = await context.newPage();
    const errors = [];
    page.on('pageerror', error => errors.push(error.message));
    page.on('console', message => { if (message.type() === 'error') console.error('Browser error:', message.text()); });
    page.on('response', async response => { if (response.url().includes('/api/v1/') && response.status() >= 400) console.log('API result:', response.url(), response.status(), await response.text()); });
    await page.goto(base + '/manage/');
    await page.locator('#loaded-room').getByText('panel-test-room', { exact: true }).waitFor();
    assert.equal(await page.locator('#state').textContent(), 'DISABLED');
    await page.locator('#connect-room').fill('panel-test-room');
    await page.locator('#connect-token').fill('browser-test-api-token');
    await page.locator('#save-connection').click();
    await page.getByText('Connection settings saved.', { exact: true }).waitFor();
    assert.equal(await page.locator('#connect-token').inputValue(), '');
    assert.equal(await page.locator('#token-state').textContent(), 'Token saved');
    const savedConnection = await page.evaluate(async () => (await fetch('/api/v1/bot/connection')).json());
    assert.deepEqual(savedConnection, {roomId:'panel-test-room',tokenConfigured:true,autoConnect:false});
    await page.locator('#emote-search').fill('blackpink');
    assert.equal(await page.locator('#emote-selector option').count(), 1);
    await page.locator('#emote-search').fill('');
    assert(await page.locator('#emote-selector option').count() > 50);
    let connectRequest;
    await page.route('**/api/v1/bot/connect', async route => {
      connectRequest = route.request().postDataJSON();
      await route.fulfill({status:200,contentType:'application/json',body:JSON.stringify({state:'CONNECTING',roomId:'panel-test-room',queuedMessages:0})});
    });
    await page.locator('#connection-form button[type=submit]').click();
    await page.getByText('Connection requested. Wait for READY before sending live commands.', {exact:true}).waitFor();
    assert.deepEqual(connectRequest, {roomId:'panel-test-room',apiToken:null,autoConnect:false});
    await page.unroute('**/api/v1/bot/connect');
    await page.locator('[name=welcomeMessage]').fill('Browser test {username}');
    await page.getByRole('button', { name: 'Save settings' }).click();
    await page.getByText('Room settings saved.', { exact: true }).waitFor();
    await page.locator('#admin-user').fill('panel-owner-id');
    await page.locator('#admin-role').selectOption('OWNER');
    console.log('Admin form:', await page.locator('#admin-form').evaluate(form => ({ valid: form.checkValidity(), values: [...form.elements].map(e => ({id:e.id,value:e.value,invalid:e.validationMessage})) })));
    await page.getByRole('button', { name: 'Save administrator' }).click();
    await page.getByText('Administrator saved.', { exact: true }).waitFor();
    await page.locator('#admin-list').getByText('panel-owner-id').waitFor();
    await page.reload();
    await page.locator('#loaded-room').getByText('panel-test-room', { exact: true }).waitFor();
    assert.equal(await page.locator('[name=welcomeMessage]').inputValue(), 'Browser test {username}');
    await page.locator('#admin-list').getByText('panel-owner-id').waitFor();
    assert.equal(await page.locator('#token-state').textContent(), 'Token saved');
    assert.equal(await page.locator('#connect-token').inputValue(), '');
    await page.locator('#emote-id').fill('dance-new-api-emote');
    await page.getByRole('button', { name: 'Play emote' }).click();
    await page.getByText('Bot operation unavailable. Check connection status and try again.', { exact: true }).waitFor();
    assert.equal(await page.locator('#loops-count').textContent(), '0');
    fs.mkdirSync('target/panel-check', { recursive: true });
    await page.screenshot({ path: 'target/panel-check/desktop.png', fullPage: true });
    assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), 'Desktop overflows horizontally.');
    await page.setViewportSize({ width: 390, height: 844 });
    await page.screenshot({ path: 'target/panel-check/mobile.png', fullPage: true });
    assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), 'Mobile overflows horizontally.');
    await page.locator('#disconnect').click();
    await page.getByText('Disconnected. Automatic connection disabled.', {exact:true}).waitFor();
    assert.equal(await page.locator('#state').textContent(), 'STOPPED');
    await page.locator('#forget-token').click();
    await page.getByText('Token removed. Bot disconnected.', {exact:true}).waitFor();
    assert.equal(await page.locator('#token-state').textContent(), 'No token configured');
    assert.deepEqual(errors, []);
    console.log('Panel passed: real PostgreSQL connection/settings/admin persistence, masked credentials, connection form, searchable catalog, Basic auth, CSRF writes, offline errors, desktop/mobile layout.');
  } catch (error) {
    if (page) {
      fs.mkdirSync('target/panel-check', { recursive: true });
      await page.screenshot({ path: 'target/panel-check/failure.png', fullPage: true });
      console.error('Panel state:', await page.locator('body').innerText());
    }
    throw error;
  } finally { if (browser) await browser.close(); app.kill('SIGTERM'); }
})().catch(error => { console.error(error); console.error(fs.readFileSync('/tmp/hexorise-panel-server.log', 'utf8')); process.exitCode = 1; });
