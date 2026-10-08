# Hexorise

پایه ربات هایرایز با **Java 17، Spring Boot 4.1.1 و PostgreSQL 17**.

مخزن فعال پروژه: [Tahanoa/hexorise](https://github.com/Tahanoa/hexorise). این پروژه ادامه پایه ساخته‌شده در `hexora_highrise_bot` است و از Spring Boot 4، Jackson 3 و اسکلت مخزن جدید استفاده می‌کند.

## قابلیت‌ها

- اتصال مستقیم WebSocket جاوا با هدرها و قالب پیام تأییدشده در SDK رسمی 25.1.0.
- دریافت نشست، چت، whisper، ورود و خروج؛ keepalive، timeout و reconnect با تأخیر افزایشی.
- توقف reconnect برای خطای `do_not_reconnect` و پاسخ HTTP 401/403.
- صف خروجی محدود، تطبیق پاسخ با `rid` و timeout درخواست؛ خواندن نرخ از `SessionMetadata`.
- خوشامدگویی قابل تنظیم و فرمان‌های `!help`، `!راهنما`، `!ping` و `!پینگ` با cooldown.
- ذخیره تنظیمات هر اتاق با Spring Data JPA در PostgreSQL و مهاجرت نسخه‌بندی‌شده با Flyway.
- API مدیریت با HTTP Basic و CSRF، اعتبارسنجی ورودی، وضعیت اتصال و health checks.
- Docker، Maven Wrapper و CI همراه تست با PostgreSQL واقعی.

این نسخه برای **یک ربات و یک اتاق فعال در هر process** ساخته شده است. تنظیمات و مدیران چند اتاق را می‌توان ذخیره کرد؛ عملیات زنده همیشه روی اتاق `HIGHRISE_ROOM_ID` انجام می‌شوند.

- انتخاب ایموت با نام، شماره یا شناسه از کاتالوگ؛ اجرای تکی، حلقه دوره‌ای و توقف.
- اخراج، ban، unban و mute با مجوز مدیر؛ محافظت از ربات و مدیران ذخیره‌شده.
- ضداسپم برای پیام‌های تکراری و پشت‌سرهم؛ مسدودکردن موقت فرمان‌ها و mute اختیاری.
- ذخیره خوشامد، مدیران OWNER/ADMIN و تنظیمات ضداسپم/ایموت هر اتاق در PostgreSQL.
- پنل مدیریت واکنش‌گرا با تم تیره و سبز نئونی، HTTP Basic و CSRF.

## پنل مدیریت و فرمان‌ها

پس از اجرا، [پنل مدیریت](http://localhost:8080/manage/) را باز کن و با `ADMIN_USERNAME` و `ADMIN_PASSWORD` وارد شو. شناسه اتاق را بارگذاری کن و اولین مدیر را با شناسه کاربر و نقش `OWNER` ثبت کن. ورود پنل مستقل از نقش‌های داخل اتاق است؛ افراد ثبت‌شده در جدول مدیران اجازه استفاده از فرمان‌های مدیریتی چت را دارند.

| فرمان با پیشوند پیش‌فرض | کاربرد و مجوز |
| --- | --- |
| `!help`, `!ping`, `!emotes` | راهنما، وضعیت و فهرست شماره/نام ایموت‌ها |
| `!emote hello` یا `!emote 2` | اجرای تکی روی کاربر ارسال‌کننده |
| `!dance macarena 10` یا `!dance 1` | اجرای دوره‌ای روی کاربر؛ فاصله اختیاری ۲ تا ۳۰۰ ثانیه |
| `!stop` | لغو حلقه خود کاربر؛ حتی هنگام cooldown یا مسدودشدن ضداسپم |
| `!botdance 1 10`, `!botstop` | حلقه روی ربات و توقف؛ ADMIN/OWNER |
| `!kick @username` | اخراج؛ ADMIN/OWNER |
| `!ban user-id [seconds]`, `!unban user-id` | مسدودسازی با مدت اختیاری و رفع آن؛ ADMIN/OWNER |
| `!mute @username [seconds]` | بی‌صداکردن، پیش‌فرض ۶۰ ثانیه؛ ADMIN/OWNER |
| `!admin add user-id`, `!admin remove user-id` | تغییر مدیر ADMIN؛ فقط OWNER |

نام کاربری باید یکتا و در فهرست اعضای دریافت‌شده باشد؛ شناسه کاربر برای افراد خارج اتاق نیز قابل استفاده است. تغییر OWNER از پنل انجام می‌شود. خود Highrise مجوز ربات را برای هر عملیات بررسی می‌کند؛ ثبت مدیر در برنامه جایگزین مجوز اتاق نیست.

کاتالوگ اولیه شامل پنج ایموت تأییدشده است و در `src/main/resources/emotes.json` قابل توسعه است. تنها یک حلقه برای هر هدف و حداکثر ۵۰ حلقه فعال ایجاد می‌شود. هر اجرای بعدی پس از پاسخ API و فاصله تعیین‌شده آغاز می‌شود و همه فرمان‌ها از صف و محدودیت نرخ مشترک استفاده می‌کنند. قطع اتصال یا غیرفعال‌کردن ایموت در اتاق فعال حلقه‌ها را متوقف می‌کند. توقف، ارسال‌های آینده و درخواست‌های هنوز ارسال‌نشده را لغو می‌کند؛ انیمیشن قبلاً ارسال‌شده ممکن است تا پایان اجرا شود.

ضداسپم پیش‌فرض در پنجره ۱۰ ثانیه‌ای حداکثر ۶ پیام و ۳ تکرار را می‌پذیرد و فرمان‌های کاربر متخلف را ۲۰ ثانیه مسدود می‌کند. مدیران از ضداسپم معاف هستند. mute خودکار پیش‌فرض خاموش است و مجوز Highrise لازم دارد. شمارنده‌های کوتاه‌مدت و حلقه‌ها در حافظه‌اند؛ تنظیمات و مدیران در PostgreSQL ماندگارند.

تمام فایل‌های Java انگلیسی هستند؛ پیام خوشامد و نام‌های فارسی فرمان‌ها در `bot-messages.properties` قرار دارند.

تنظیمات اصلی در `src/main/resources/application.properties` قرار دارند. تنها دیتابیس پشتیبانی‌شده PostgreSQL است؛ driver آن صریحاً تنظیم شده و دیتابیس جایگزین یا درون‌حافظه‌ای وجود ندارد.

## اجرای سریع با Docker

```bash
cp .env.example .env
# مقادیر POSTGRES_PASSWORD و ADMIN_PASSWORD را تغییر بده (رمز مدیر حداقل ۱۶ کاراکتر).
docker compose up --build -d
```

در حالت پیش‌فرض `HIGHRISE_ENABLED=false` است؛ برنامه و دیتابیس بدون توکن ربات اجرا می‌شوند.
برای ورود ربات، در `.env` مقادیر زیر را تنظیم و برنامه را دوباره ایجاد کن:

```dotenv
HIGHRISE_ENABLED=true
HIGHRISE_ROOM_ID=your-room-id
HIGHRISE_API_TOKEN=your-bot-api-token
```

```bash
docker compose up -d --force-recreate app
curl http://localhost:8080/actuator/health
```

دسترسی ساخت ربات و مجوز اتاق باید در حساب Highrise فعال باشد. مقادیر توکن را فقط در محیط اجرا نگه دار؛ در گیت یا اسکرین‌شات قرار نده.
Docker دیتابیس و HTTP را فقط روی localhost منتشر می‌کند. برای دسترسی بیرونی یک reverse proxy با HTTPS اضافه کن؛ HTTP Basic باید روی TLS استفاده شود.

## اجرای محلی بدون Docker برای برنامه

JDK 17 یا جدیدتر، PostgreSQL و اینترنت برای دریافت وابستگی‌های Maven لازم است.

```bash
cp .env.example .env
# رمزها را در .env تکمیل کن.
docker compose up -d postgres
set -a
. ./.env
set +a
./mvnw spring-boot:run
```

Spring Boot به‌تنهایی `.env` را بارگذاری نمی‌کند؛ shell یا IDE باید متغیرها را وارد کند. در PowerShell متغیرها را با `$env:ADMIN_PASSWORD` و سایر نام‌ها تنظیم و `mvnw.cmd spring-boot:run` اجرا کن.
`DATABASE_URL` در صورت تنظیم باید JDBC URL باشد، برای مثال `jdbc:postgresql://localhost:5432/hexora_highrise`.

## API مدیریت

| مسیر | کاربرد |
| --- | --- |
| `GET /actuator/health` | وضعیت کلی برنامه و PostgreSQL؛ عمومی بدون جزئیات حساس |
| `GET /api/v1/bot/status` | وضعیت ربات، شناسه اتاق/اتصال و تعداد پیام‌های صف |
| `GET /api/v1/rooms/{roomId}/settings` | تنظیمات ذخیره‌شده یا پیش‌فرض بدون ایجاد رکورد |
| `PUT /api/v1/rooms/{roomId}/settings` | ثبت تنظیمات؛ نیازمند احراز هویت و CSRF |
| `GET /api/v1/emotes` | کاتالوگ شماره/نام/شناسه ایموت |
| `GET /api/v1/bot/users`, `GET /api/v1/bot/loops` | اعضای دریافت‌شده و حلقه‌های فعال |
| `POST /api/v1/bot/emotes` | `selector`, `targetUserId` اختیاری، `repeat`, `intervalSeconds` اختیاری |
| `POST /api/v1/bot/emotes/stop` | توقف هدف؛ `targetUserId=null` برای ربات |
| `POST /api/v1/bot/emotes/stop-all` | توقف همه حلقه‌ها |
| `POST /api/v1/bot/moderation` | `userId`, `action`, `durationSeconds` اختیاری |
| `GET /api/v1/rooms/{roomId}/admins` | فهرست مدیران |
| `PUT /api/v1/rooms/{roomId}/admins/{userId}` | ثبت/تغییر نقش با `{"role":"OWNER"}` یا ADMIN |
| `DELETE /api/v1/rooms/{roomId}/admins/{userId}` | حذف مدیر |
| `GET /api/v1/csrf` | دریافت توکن CSRF؛ کوکی پاسخ را برای درخواست تغییر نگه دار |

تمام `/api/v1/**` نیازمند نام کاربری/رمز مدیر هستند. برای نمونه‌ها متغیرهای `.env` باید در shell بارگذاری شده باشند:

```bash
curl -u "$ADMIN_USERNAME:$ADMIN_PASSWORD" http://localhost:8080/api/v1/bot/status
curl -u "$ADMIN_USERNAME:$ADMIN_PASSWORD" -c /tmp/hexora-cookies.txt \
  http://localhost:8080/api/v1/csrf
```

توکن پاسخ را در متغیر `CSRF_TOKEN` قرار بده و همان کوکی را برای PUT بفرست:

```bash
curl -u "$ADMIN_USERNAME:$ADMIN_PASSWORD" -b /tmp/hexora-cookies.txt \
  -X PUT http://localhost:8080/api/v1/rooms/your-room-id/settings \
  -H "X-CSRF-TOKEN: $CSRF_TOKEN" -H 'Content-Type: application/json' \
  -d '{"welcomeEnabled":true,"welcomeMessage":"سلام {username}، خوش آمدی!","commandPrefix":"!","commandCooldownSeconds":3}'
```

محدودیت‌ها: پیام خوشامد حداکثر ۲۵۵ کاراکتر، پیشوند بدون فاصله و حداکثر ۸ کاراکتر، cooldown بین ۱ تا ۳۰۰ ثانیه. `{username}` در پیام جایگزین می‌شود. صف حداکثر ۱۰۰ پیام دارد؛ پیام‌های صف‌شده هنگام قطع اتصال شکست می‌خورند و خودکار دوباره پخش نمی‌شوند.

`READY` فقط پس از دریافت نشست نمایش داده می‌شود. health برنامه جایگزین وضعیت اتصال ربات نیست. اگر `REJECTED` دیده شد، مجوزها و توکن را اصلاح و process را دوباره اجرا کن.

## ساخت و تست

```bash
./mvnw verify
# تست دیتابیس واقعی، پس از آماده‌شدن PostgreSQL و بارگذاری متغیرها:
RUN_DB_TESTS=true ./mvnw verify
./mvnw package
java -jar target/hexorise-0.1.0-SNAPSHOT.jar
```

تست دیتابیس بدون `RUN_DB_TESTS=true` رد می‌شود؛ GitHub Actions آن را با PostgreSQL فعال اجرا می‌کند. تست‌ها قالب پروتکل، نرخ، cooldown، whisper، خوشامدگویی، ضداسپم، زمان‌بندی و توقف ایموت، مجوز مدیریت، احراز هویت، CSRF، اعتبارسنجی و ماندگاری تنظیمات/مدیران را پوشش می‌دهند.

## ساختار

```text
src/main/java/org/example/hexorise/
  config/    تنظیمات و امنیت
  client/    اتصال، پروتکل و چرخه عمر
  bot/       رویدادها و فرمان‌ها، ایموت، مجوز و ضداسپم
  room/      تنظیمات ماندگار اتاق
  api/       API مدیریت
src/main/resources/db/migration/  مهاجرت PostgreSQL
src/test/                        تست‌ها
```

## مراجع و محدودیت بررسی

- [راهنمای فارسی API](docs/Highrise-Bot-API-Guide-FA.md)
- [کد اتصال SDK رسمی](https://github.com/pocketzworld/python-bot-sdk/blob/de928597846e678ef14213dbe8516bdfb655ec51/src/highrise/__main__.py)
- [مدل‌های رسمی پروتکل](https://github.com/pocketzworld/python-bot-sdk/blob/de928597846e678ef14213dbe8516bdfb655ec51/src/highrise/models.py)
- [ساخت ربات](https://create.highrise.game/learn/bots/guides/creating-a-bot)

مسیر تأییدشده از SDK `wss://highrise.game/web/botapi` است؛ هدرها `room-id` و `api-token` هستند. این بررسی، بخش نامشخص اتصال خام در راهنمای پیوست را تکمیل می‌کند. پیاده‌سازی Java یک SDK رسمی Highrise نیست.
**اتصال به اتاق واقعی بدون توکن و Room ID آزمایش نشده است.** معیار تأیید نهایی: ورود به اتاق آزمایشی، دریافت `SessionMetadata` و پاسخ به `!ping`.
