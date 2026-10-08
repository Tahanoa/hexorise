# راهنمای API و اتصال ربات Highrise
تاریخ بررسی: ۸ اکتبر ۲۰۲۶

این فایل، نقشه مستندات رسمی و راهنمای شروع توسعه است؛ آرشیو کامل تمام صفحات سایت نیست. لینک‌ها و نکات تأییدشده از منابع رسمی آمده‌اند. پیشنهادهای معماری با عنوان «پیشنهاد» مشخص شده‌اند. اتصال زنده آزمایش نشده، چون توکن ربات در اختیار نیست.

## ۱. کدام API برای چه کاری است؟

| رابط | کاربرد | نوع اتصال |
| --- | --- | --- |
| Bot API | حضور ربات در اتاق، رویدادها و اجرای فرمان | WebSocket و SDK |
| Web API | دریافت داده‌های عمومی کاربران، اتاق‌ها، پست‌ها، آیتم‌ها و grabs | HTTP با خروجی JSON |
| Studio API | برنامه‌نویسی تجربه‌ها در Highrise Studio | مسیر جدا از ربات اتاق |

مرجع Bot API: https://create.highrise.game/learn/bots/api/overview
مرجع Web API: https://create.highrise.game/learn/web-api/general/overview
درگاه مستندات و Studio: https://create.highrise.game/learn

Web API عمومی جایگزین دسترسی Bot API نیست. طبق صفحه Overview، داده‌های عمومی آن بدون API Key خوانده می‌شوند و ویرایش داده از این رابط عمومی پشتیبانی نمی‌شود. اگر endpoint دیگری احراز هویت می‌خواهد، شرایط همان endpoint ملاک است.

## ۲. پیش‌نیاز اتصال ربات

برای اجرا باید Bot ایجاد شده، API Token و Room ID داشته باشی. در اتاق متعلق به دیگران، ربات به مجوز Designer نیاز دارد. Room ID را از «Share this Room» در اطلاعات اتاق بگیر. درگاه ایجاد ربات:
https://create.highrise.game/dashboard/credentials/api-keys
مسیر Settings که پشتیبانی معرفی کرده:
https://highrise.game/account/settings

راهنمای رسمی:
https://create.highrise.game/learn/bots/guides/creating-a-bot

برای حساب taha_bioty در این گفتگو، ساخت ربات هنوز محدود است. آماده‌کردن کد ممکن است، اما ساخت حساب ربات و دریافت توکن به فعال‌شدن دسترسی وابسته است. پاسخ‌های پشتیبانیِ ارائه‌شده زمان قطعی برای فعال‌شدن اعلام نکرده‌اند.

## ۳. SDK رسمی و نسخه فعلی

SDK رسمی Python:
https://pypi.org/project/highrise-bot-sdk/
مخزن رسمی:
https://github.com/pocketzworld/python-bot-sdk

نسخه فعلی نمایش‌داده‌شده در PyPI: **25.1.0**، منتشرشده در ۲ آوریل ۲۰۲۶.
حداقل Python در metadata بسته: **3.10**؛ برای شروع می‌توان Python 3.11 را انتخاب کرد.
این نسخه پشتیبانی از آپلود رسانه را اضافه کرده و رفتار reconnect پس از بسته‌شدن اتصال توسط سرور را اصلاح کرده است.

برخی آموزش‌های Windows/Mac هنوز 24.1.0 یا توصیه‌های قدیمی Python را نشان می‌دهند؛ برای نسخه نصب، PyPI را ملاک بگیر. معرفی JavaScript در صفحه Overview به معنی تأیید یک بسته مشخص npm نیست؛ SDK رسمی Node.js در منابع بررسی‌شده تأیید نشد.

## ۴. شروع کار با Python

راهنمای Windows:
https://create.highrise.game/learn/bots/guides/local/windows
راهنمای Mac:
https://create.highrise.game/learn/bots/guides/local/mac

دستورهای زیر را در پوشه پروژه اجرا کن:

```bash
python -m venv .venv
```

فعال‌کردن محیط در Windows CMD:

```bat
.venv\Scripts\activate
```

فعال‌کردن در Linux/macOS:

```bash
source .venv/bin/activate
python -m pip install highrise-bot-sdk==25.1.0
```

پس از فعال‌کردن محیط Windows نیز همان دستور pip را اجرا کن.

فایل mybot.py را با این نمونه آموزشی بساز. این نمونه ثبت شروع نشست و دریافت چت را نشان می‌دهد و پاسخی ارسال نمی‌کند:

```python
from highrise import BaseBot, SessionMetadata, User

class Bot(BaseBot):
    async def on_start(self, session_metadata: SessionMetadata) -> None:
        print("Highrise session started")

    async def on_chat(self, user: User, message: str) -> None:
        print(f"{user.username}: {message}")
```

اجرا پس از دریافت توکن:

```bash
highrise mybot:Bot <ROOM_ID> <BOT_API_TOKEN>
```

mybot نام ماژول بدون پسوند .py و Bot نام کلاس است. مقادیر داخل <> باید جایگزین شوند. نمونه از ساختار callbackهای مستندات استفاده می‌کند؛ در این بررسی اجرا و اتصال شبکه آن آزمایش نشده است.

مرجع نمونه:
https://create.highrise.game/learn/bots/guides/examples/basics

## ۵. اتصال مستقیم WebSocket

سرور معرفی‌شده در مرجع رسمی **production.highrise.game** با پروتکل **wss** است. راهنمای ساخت ربات، اتصال مستقیم WebSocket و Python SDK را دو روش معادل معرفی می‌کند.

راهنما برای انتخاب رویدادها پارامتر زیر را معرفی می‌کند:

```text
?events=chat,user_joined
```

**این عبارت URL کامل اتصال نیست.** مسیر دقیق WebSocket، نام headerهای احراز هویت و قالب کامل handshake از منابع قابل‌دسترسی این بررسی تأیید نشدند؛ بنابراین نمونه خامی با header یا مسیر حدسی ارائه نشده است.

برای پیاده‌سازی بدون SDK، ابتدا کد اتصال نسخه منتخب SDK را بررسی کن: URL، headers، پیام‌های JSON، keepalive، ارتباط درخواست و پاسخ و خطاها. سپس یک اتصال کوچک را در اتاق آزمایشی اجرا کن. در شروع پروژه، SDK رسمی مقدار زیادی از این کار را پوشش می‌دهد.

## ۶. نشست، رویدادها و محدودیت درخواست

SessionMetadata اولین پیام نشست است و اطلاعات ربات، اتاق، connection_id و rate_limits را می‌دهد. connection_id برای گزارش خطا مفید است. نرخ را از نشست بخوان و عدد ثابت را برای همه حساب‌ها فرض نکن. مستندات الگوریتم leaky bucket و قاعده عمومی حدود یک درخواست در ثانیه را توضیح می‌دهند؛ این قاعده جایگزین نرخ واقعی نشست نیست.

مرجع:
https://create.highrise.game/learn/bots/api/endpoints/sessionmetadata

| نام اشتراک رویداد | کاربرد |
| --- | --- |
| chat | چت و whisper |
| emote | حرکات نمایشی |
| reaction | واکنش‌ها |
| user_joined / user_left | ورود و خروج |
| user_moved | تغییر موقعیت |
| tip_reaction | رویداد tip |
| voice | تغییرات voice |
| channel | پیام‌های کانال پنهان |

نام اشتراک WebSocket با نام callback SDK یکسان نیست. نمونه رسمی callbackهایی مانند on_start، on_chat، on_whisper، on_user_join، on_user_leave، on_emote، on_reaction، on_tip، on_user_move و on_channel دارد. امضای دقیق هر callback را از SDK و نمونه همان نسخه بگیر.

پیشنهاد: کارهای HTTP و ذخیره‌سازی طولانی را از مسیر دریافت رویداد جدا کن؛ فرمان‌های خروجی را از صف محدودشده عبور بده تا burst پیام ایجاد نشود.

## ۷. نقشه قابلیت‌ها و صفحات مرجع

| قابلیت | صفحه رسمی |
| --- | --- |
| کاربران حاضر و موقعیت آن‌ها | https://create.highrise.game/learn/bots/api/endpoints/getroomusersrequest |
| اجرای emote | https://create.highrise.game/learn/bots/api/endpoints/emoterequest |
| حرکت روی کف | https://create.highrise.game/learn/bots/api/endpoints/floorhitrequest |
| حرکت به Anchor مبلمان | https://create.highrise.game/learn/bots/api/endpoints/anchorhitrequest |
| پیام پنهان بین ربات‌ها/اسکریپت‌ها | https://create.highrise.game/learn/bots/api/endpoints/channelrequest |
| وضعیت voice | https://create.highrise.game/learn/bots/api/endpoints/checkvoicechatrequest |
| kick / ban / unban / mute | https://create.highrise.game/learn/bots/api/endpoints/moderateroomrequest |
| موجودی کیف ربات | https://create.highrise.game/learn/bots/api/endpoints/getwalletrequest |
| پیام‌های یک conversation | https://create.highrise.game/learn/bots/api/endpoints/getmessagesrequest |
| ارسال پیام و دعوت | https://create.highrise.game/learn/bots/api/endpoints/sendmessagerequest |
| لباس، موجودی و خرید آیتم | https://create.highrise.game/learn/bots/guides/change-bot-appearance |

این جدول قابلیت‌های مهم را پوشش می‌دهد، نه همه request/responseهای پروتکل. نام endpoint را مستقیماً نام متد Python فرض نکن. هر عملیات همچنان تابع مجوز اتاق، اعتبار داده و محدودیت سرور است.

ChannelRequest پیام را در چت عمومی نمایش نمی‌دهد و message و tags را مستند کرده است. پنهان‌بودن از چت به معنی رمزنگاری یا کانال محرمانه نیست.

ModerateRoomRequest شناسه کاربر، moderation_action و action_length اختیاری دارد. پیشنهاد: فرمان مدیریتی را فقط برای شناسه کاربران مجاز فعال کن، نه صرفاً نام نمایشی آن‌ها.

GetMessagesRequest حداکثر ۲۰ پیام برمی‌گرداند؛ برای صفحه بعد last_message_id و برای انتخاب گفتگو conversation_id لازم است. SendMessageRequest به conversation_id، content و type وابسته است؛ دعوت اتاق room_id می‌خواهد. Whisper اتاق با Inbox conversation یک قابلیت واحد نیست.

## ۸. پوشش API عمومی HTTP

آدرس پایه:
https://webapi.highrise.game/

| منبع | کاربرد |
| --- | --- |
| users | اطلاعات عمومی حساب |
| rooms | مشخصات اتاق |
| posts | محتوای پست‌ها |
| items | اطلاعات آیتم‌ها |
| grabs | اطلاعات grabs |

خواندن نمونه فهرست اتاق‌ها:

```bash
curl "https://webapi.highrise.game/rooms?limit=50"
```

صفحه‌بندی عمومی از starts_after و ends_before استفاده می‌کند. پاسخ HTTP 429 یعنی محدودیت نرخ؛ پیشنهاد: retry با تأخیر افزایشی و cache برای داده‌های تکراری.

مراجع جزئی:
- کاربر: https://create.highrise.game/learn/web-api/endpoints/invoke_get_user
- اتاق: https://create.highrise.game/learn/web-api/endpoints/invoke_get_room
- فهرست مدل‌های داده: https://create.highrise.game/learn/web-api

برای ادامه، schema پاسخ هر endpoint را بخوان؛ ساختار کاربران، room و item را یکسان فرض نکن.

## ۹. لباس، tip، voice و چند ربات

راهنمای ظاهر توضیح می‌دهد که آیتم باید رایگان یا در inventory ربات باشد؛ outfit حداقل بخش‌های لازم بدن، صورت و لباس را می‌خواهد. خرید با gold محدود به آیتم‌های قابل‌خرید فروشگاه است و ربات برای معامله آیتم طراحی نشده است. در منابع نام متد لباس یکدست نیست؛ API نسخه نصب‌شده را بررسی کن.

راهنمای tipping:
https://create.highrise.game/learn/bots/guides/examples/tipping-bot
راهنمای دریافت tips:
https://create.highrise.game/learn/bots/guides/earn-gold-with-a-bot

مستندات نسخه SDK قابلیت voice، گفتگوهای مستقیم، چند ربات در یک process و چند اتاق را ذکر می‌کنند. بنابراین حرف گروه که هر ربات حتماً کد و سرور جدا می‌خواهد، الزام عمومی SDK نیست. برای هر هویت ربات اعتبار اتصال مربوط به خودش لازم است؛ طراحی اجرای چند نشست را با SDK همان نسخه بررسی کن.

Voice API را API پخش مستقیم فایل صوتی یا YouTube فرض نکن؛ صفحات بررسی‌شده مدیریت وضعیت و کاربران voice را تأیید می‌کنند. امکان مشخص radio/media را باید جداگانه از مرجع مربوط به آن بررسی کرد.

## ۱۰. نمونه‌های رسمی برای ادامه

- نمونه پایه و callbackها: https://create.highrise.game/learn/bots/guides/examples/basics
- ذخیره آمار فعالیت: https://create.highrise.game/learn/bots/guides/examples/persistent-data
- اتصال به API بیرونی: https://create.highrise.game/learn/bots/guides/examples/third-party-integration
- فهرست نمونه‌ها: https://create.highrise.game/learn/bots/guides/examples/overview
- میزبانی Replit: https://create.highrise.game/learn/bots/guides/cloud/replit

راهنمای Replit اصطلاحات و تعرفه‌های قدیمی مثل Always On را دارد؛ آن را الگوی مفهومی اجرا بدان، نه مرجع قیمت یا امکانات فعلی میزبان.

## ۱۱. پیشنهاد معماری اجرا

برای نسخه اول: تنظیمات → اتصال SDK → callbackها → تشخیص فرمان → بررسی مجوز → صف ارسال.
برای داده‌های ماندگار از پایگاه داده استفاده کن؛ برای پروژه کوچک SQLite و برای سرویس چندنمونه‌ای یک پایگاه داده مشترک قابل بررسی است.

توکن را در secret میزبان یا متغیر محیطی نگه دار، از مخزن کد و log دور نگه دار و در صورت افشا آن را regenerate کن. ربات یک برنامه در حال اجراست؛ بسته‌شدن process یا خاموش‌شدن رایانه اجرای آن را متوقف می‌کند. برای فعالیت دائمی، process manager، سیاست restart و ثبت خطا لازم است. پس از reconnect، وضعیت کاربران و داده‌های وابسته به اتاق را دوباره دریافت کن.

برای اتصال API بیرونی از درخواست async با timeout و cache استفاده کن. این پیشنهادهای مهندسی، الزام اعلام‌شده Highrise نیستند.

## ۱۲. عیب‌یابی و مسیر ساخت پیشنهادی

| نشانه | بررسی اولیه |
| --- | --- |
| امکان ساخت ربات وجود ندارد | eligibility حساب؛ مستقل از نصب SDK |
| ربات وارد اتاق نمی‌شود | Token، Room ID و Designer permissions |
| import یا فرمان highrise پیدا نمی‌شود | محیط مجازی فعال و نسخه بسته |
| برخی فرمان‌ها رد می‌شوند | مجوز عملیات و schema ورودی |
| تأخیر یا HTTP 429 | نرخ واقعی، صف خروجی و retry |
| بعد از خاموش‌شدن سیستم ربات می‌رود | میزبان و process دائمی |
| تغییر لباس رد می‌شود | موجودی آیتم و حداقل اجزای outfit |

پیشنهاد مراحل: ثبت نشست و چت → فرمان help با cooldown → خوشامدگویی → تنظیمات ماندگار → فرمان‌های مدیریتی → tip یا قابلیت‌های دلخواه → میزبانی دائمی.

در این بررسی endpoint خام اتصال و headerهای handshake قابل تأیید نبودند؛ SDK و نمونه آموزشی نیز با توکن واقعی آزمایش نشده‌اند. لینک‌های مرجع برای مراجعه و بررسی نسخه آینده ضمیمه شده‌اند.

## ۱۳. پشتیبانی توسعه‌دهندگان

- Discord معرفی‌شده توسط پشتیبانی: https://discord.gg/highrise
- bot-api برای کمک جامعه توسعه‌دهندگان.
- درخواست دسترسی BETA API discussion طبق راهنمایی پشتیبانی همین گفتگو.
- انجمن رسمی: https://createforum.highrise.game/

بازشدن کانال دیسکورد به‌خودی‌خود به معنی فعال‌شدن ساخت ربات در حساب نیست.
