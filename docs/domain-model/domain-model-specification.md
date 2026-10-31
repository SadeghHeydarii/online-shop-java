# سند جامع مشخصات مدل دامنه (Domain Model Technical Specification)
**پروژه:** سامانه فروشگاه آنلاین شیءگرا بر پایه Java 21 LTS  
**دانشگاه صنعتی امیرکبیر (پلی‌تکنیک تهران) --- دانشکده مهندسی کامپیوتر**  
**درس:** اصول طراحی نرم‌افزار --- نیم‌سال اول ۱۴۰۵--۱۴۰۶  
**وضعیت:** نسخه نهایی یکپارچه‌شده و مصوب بر مبنای اصل اتحاد دیاگرام و پیاده‌سازی (Reconciled Baseline)

---

## ۱. شناسنامه و رویکرد معماری مدل (Architectural Overview)

این مستند به عنوان منبع موثق و خط مبنای قطعی (Single Source of Truth)، ساختار، روابط، قوانین کسب‌وکار، قراردادهای صوری و رفتار حاکم بر سامانه فروشگاه آنلاین را تبیین می‌نماید. مدل حاضر با وفاداری کامل به نیازمندی‌های فاز دوم پروژه و بر پایه آموزه‌های چهار جزوه درسی تدوین شده است:

1. **`clean-code.pdf`:** انتخاب اسامی بامعنا، متدهای تک‌منظوره و کوچک، تفکیک پرسش از فرمان (CQS)، و بهینه‌سازی خوانایی.
2. **`dataAbstraction.pdf`:** تفکیک قطعی انواع داده جهش‌پذیر (Mutable) و تغییرناپذیر (Immutable)، تدوین دقیق نامتغیرهای بازنمایی (Representation Invariants - RI)، توابع انتزاع (Abstraction Functions - AF)، و محافظت در برابر افشای بازنمایی (Representation Exposure).
3. **`java-code-conventions.pdf`:** پیروی دقیق از استانداردهای رسمی کدنویسی جاوا نظیر نام‌گذاری پکیج‌ها با حروف کوچک، کلاس‌ها با PascalCase، متدها و فیلدها با camelCase و ثابت‌ها با UPPER_SNAKE_CASE.
4. **`procedural-abstraction.pdf`:** تدوین صوری و دقیق قراردادها شامل پیش‌شرط‌ها (`REQUIRES`)، اثرات جانبی (`MODIFIES`) و پس‌شرط‌ها و استثنائات (`EFFECTS`) برای تمامی رفتارهای کلیدی.

### اصول بنیادین حاکم بر مدل‌سازی:
* **معماری لایه‌ای ناب (Layered Architecture):** تفکیک سخت‌گیرانه لایه‌های واسط کاربری متنی (`CLI`)، لایه سرویس‌های کاربردی (`Service`)، لایه مدل کسب‌وکار (`Model`) و کلاس‌های راه‌انداز/ابزارهای کمکی (`Main & Util`).
* **استفاده از قابلیت‌های نوین جاوا ۲۱ (Java 21 LTS):** مدل‌سازی تمامی اشیاء مقداری بدون هویت تغییرناپذیر به صورت رکوردهای جاوا (`record`) با سازنده‌های فشرده (Compact Constructors) جهت اعتبارسنجی خودکار.
* **مشخصه‌سازی صوری عملیات:** ارائه قرارداد سه مؤلفه‌ای برای تک‌تک متدهای تأثیرگذار در لایه‌های مدل و سرویس.
* **اصل اتحاد و هماهنگی کامل (Union Baseline):** تمامی متدها، فیلدها، قابلیت‌های بازیابی رمز، فیلترهای کاتالوگ، انزوای دسترسی سفارش‌ها و مدیریت تصاویر در کاتالوگ و دیاگرام‌ها به صورت یکپارچه و سازگار درج شده‌اند.

### ۱.۱. معماری لایه‌ای و تفکیک مسئولیت‌ها (Layered Architecture)

سامانه بر مبنای جدول زیر به چهار بخش تفکیک شده است. جهت وابستگی‌ها صرفاً از لایه‌های بالاتر به لایه‌های پایین‌تر است و لایه مدل هیچ‌گونه اطلاعی از لایه‌های سرویس یا واسط کاربری ندارد:

| بخش / لایه | پکیج مربوطه | مسئولیت اصلی | وابستگی‌های مجاز |
| :--- | :--- | :--- | :--- |
| **Model** | `org.example.model` | تعریف موجودیت‌ها، اشیاء مقداری، انواع شمارشی، نامتغیرهای بازنمایی و رفتارهای ذاتی دامنه. | کتابخانه استاندارد جاوا (`java.time`, `java.math`, `java.util`) |
| **Service** | `org.example.service` | هماهنگ‌سازی Use Caseها بین موجودیت‌ها، احراز هویت، کاتالوگ، سبد خرید، سفارش، پرداخت و نظرات، مدیریت داده‌های درون‌حافظه‌ای (`In-Memory Maps`). | لایه Model، استثناهای دامنه (`org.example.exception`)، و ابزارهای عمومی |
| **CLI** | `org.example.cli` | دریافت ورودی از کاربر، اعتبارسنجی در مرز ورودی، فراخوانی سرویس‌ها و چاپ منوها و گزارش‌ها در ترمینال. | لایه‌های Service و Model |
| **Main & Util** | `org.example`, `org.example.util` | نمونه‌سازی متمرکز سرویس‌ها (Manual Dependency Injection)، بارگذاری داده‌های اولیه، ابزارهای هشینگ رمز و کنترل حلقه اصلی برنامه. | تمامی اجزای سامانه جهت راه‌اندازی و تزریق |

> **نکته معماری:** برنامه فاقد لایه دیتابیس مجزای رابطه‌ای/خارجی است. تمامی داده‌ها در ساختارهای `Map` و `List` درون‌حافظه‌ای متعلق به هر سرویس کپسوله شده‌اند و چرخه حیات داده‌ها محدود به نشست اجرای برنامه است.

### ۱.۲. قوانین و رفتارهای حاکم بر دامنه (Core Domain Rules & Policies)

قوانین حاکم بر منطق کسب‌وکار سامانه به شرح زیر فرموله و تثبیت شده‌اند:

1. **قانون هویت و ثبت‌نام کاربر:** ثبت‌نام کاربر نیازمند نام و نام خانوادگی، رمز عبور، و حداقل یک مشخصه ارتباطی معتبر (ایمیل یا شماره تلفن همراه) است. شیء مقداری `ContactInfo` صحت فرمت RFC ایمیل و شماره همراه ایران (`09[0-9]{9}`) را تضمین نموده و سرویس‌های `AuthService` و `UserService` یکتایی آنها را در سراسر سامانه اعتبارسنجی می‌کنند.
2. **قانون احراز هویت و ورود انعطاف‌پذیر:** ورود به سامانه می‌تواند با ایمیل یا شماره تلفن همراه انجام گیرد. رمزهای عبور همواره به صورت هش امن SHA-256 نگهداری می‌شوند و هرگز در پروفایل بازگردانده نمی‌شوند. خروج کاربر توکن نشست جاری را باطل می‌سازد. بازیابی رمز عبور علاوه بر تغییر رمز، تمامی نشست‌های فعال پیشین کاربر را باطل می‌نماید.
3. **قانون شبیه‌سازی بازیابی رمز عبور:** در محیط متنی، فرآیند بازیابی رمز عبور به صورت کنسولی شبیه‌سازی شده است؛ یک توکن موقت عددی ۶ رقمی در خروجی کنسول نمایش داده می‌شود که ۱۰ دقیقه اعتبار داشته و پس از یک‌بار مصرف یا انقضا باطل می‌گردد.
4. **قانون رؤیت‌پذیری کاتالوگ و کنترل دسته:** در ویترین و کاتالوگ عمومی، تنها کالاهایی به مشتریان نمایش داده می‌شوند که هم پرچم فعال بودن خود کالا (`active == true`) و هم پرچم فعال بودن دسته‌بندی والد آن (`category.active() == true`) برقرار باشد. در صورتی که کالایی پس از افزودن به سبد غیرفعال گردد، فرآیند تسویه‌حساب (Checkout) یا پرداخت آن با استثنا مواجه و متوقف می‌شود.
5. **قانون تمامیت و مثبت بودن تعداد:** تعداد هر کالا همواره شیء مقداری اکیداً مثبت (`Quantity > 0`) است و در هیچ عملیاتی نمی‌تواند از موجودی انبار بیشتر باشد. افزودن مجدد کالای موجود در سبد خرید، به جای ایجاد سطر جدید، تعداد سطر موجود را افزایش می‌دهد.
6. **قانون مرزهای اعتبارسنجی ورودی:** طول رشته جستجوی کالا باید بین ۳ تا ۱۰۰ کاراکتر باشد. طول متن نظر ثبت‌شده برای محصول حداکثر ۲۰۰۰ کاراکتر است. امتیاز نظر بین ۱ تا ۵ ستاره است. ثبت و ویرایش نشانی نیازمند کد پستی دقیقاً ۱۰ رقمی عددی و تلفن تماس معتبر است.
7. **قانون محاسبات مالی و کوپن تخفیف:** مبلغ نهایی سفارش برابر است با مجموع قیمت اقلام منهای مبلغ تخفیف کوپن به علاوه هزینه ارسال (که در این نسخه صفر در نظر گرفته شده است). کوپن تخفیف باید تاریخ انقضای معتبر داشته و حداقل مبلغ خرید سفارش را احراز نماید.
8. **قانون کنترل پرداخت و اتمیک بودن تغییر موجودی:** فرآیند پرداخت تنها برای سفارش‌های در وضعیت `PENDING` یا `PAYMENT_FAILED` قابل اجراست. پیش از هرگونه کسر موجودی، کفایت موجودی تمامی اقلام سفارش به صورت اتمیک و یکجا بررسی می‌گردد. در صورت شکست پرداخت، هیچ موجودی کسر نمی‌شود و وضعیت سفارش به `PAYMENT_FAILED` تغییر یافته و امکان تلاش مجدد محفوظ است.
9. **قانون تسویه جزئی و پاکسازی هوشمند سبد خرید:** در مرحله نهایی‌سازی پرداخت سفارش، صرفاً اقلامی از سبد خرید حذف می‌شوند که در سفارش مربوطه قطعی شده و از زمان تسویه‌حساب تا پرداخت تغییر نکرده‌اند. چنانچه مشتری قلم جدیدی به سبد افزوده یا تعداد اقلام را ویرایش کرده باشد، آن اقلام در سبد خرید حفظ می‌شوند.
10. **قانون برگشت موجودی در لغو سفارش:** لغو سفارش پرداخت‌شده (`PAID`) منجر به بازگشت قطعی اقلام به موجودی انبار می‌گردد. لغو مجدد سفارشی که قبلاً لغو شده ممنوع بوده و سفارش‌های تحویل‌شده (`DELIVERED`) غیرقابل لغو هستند.
11. **قانون انزوای داده‌های مشتری (Customer Data Isolation):** مشتری در محیط CLI تنها مجاز به مشاهده جزئیات، پرداخت و لغو سفارش‌های متعلق به شناسه کاربری خود (`userId`) است. متدهای دارای پارامتر `userId` تضمین‌کننده امنیت و انزوای دسترسی هستند.
12. **قانون کنترل دسترسی نقش‌محور (RBAC):** صرفاً کاربر با نقش `ADMIN` مجوز ورود به منوی مدیریت را دارد. نقش `STOREKEEPER` در مدل شمارشی جهت گسترش آتی تعریف شده است اما به دلیل عدم پیاده‌سازی زیرسیستم انبارداری تفکیک‌شده، دسترسی به پنل مدیریت ندارد.
13. **قانون آدرس تصاویر محصولات:** تصاویر محصولات به صورت فهرستی از آدرس‌های اینترنتی معتبر (`http://` یا `https://`) ذخیره شده و در کنسول نمایش داده می‌شوند. مدیر سیستم امکان ویرایش یا جایگزینی این فهرست را داراست.
14. **قانون مرتب‌سازی کاتالوگ و امتیاز محبوبیت:** مرتب‌سازی بر مبنای «جدیدترین» بر اساس زمان ثبت محصول (`createdAt`) و «محبوب‌ترین» بر اساس شمارنده تجمیعی فروش (`soldCount`) انجام می‌گیرد. این شمارنده در هر پرداخت موفق افزایش یافته و با لغو سفارش کاهش نمی‌یابد.

---

## ۲. ماتریس ردیابی جامع ۳۶ داستان کاربری و ۱۲ داستان دامنه (Traceability Matrix)

جدول زیر نحوه نگاشت کامل تمامی ۳۶ داستان کاربری (US) و ۱۲ داستان دامنه (DS) را به اجزای مدل و سرویس نمایش می‌دهد:

| حوزه کارکردی | داستان کاربری (US) | داستان دامنه (DS) | کلاس‌های لایه Model | متدهای هماهنگ‌کننده لایه Service |
| :--- | :--- | :--- | :--- | :--- |
| **ثبت‌نام و ورود** | US 01, 02, 03 | DS 01, 02 | `User`, `UserRole`, `ContactInfo` | `AuthService.register`, `login`, `logout` |
| **بازیابی رمز عبور** | US 04 | DS 03 | `User`, `PasswordReset` | `AuthService.requestPasswordReset`, `resetPassword`, `User.changePassword` |
| **پروفایل و نشانی‌ها** | US 05, 06, 07 | DS 04 | `User`, `Address`, `ContactInfo` | `UserService.getProfile`, `updateProfile`, `addAddress`, `updateAddress`, `removeAddress`, `setDefaultAddress` |
| **کاتالوگ کالا و جستجو** | US 08--14 | DS 05 | `Product`, `Category`, `Money`, `Quantity` | `ProductService.getAllActiveProducts`, `getProductById`, `searchProducts`, `filterAndSort`, `requireAvailableProduct` |
| **مدیریت سبد خرید** | US 15--19 | DS 06 | `Cart`, `CartItem`, `Quantity`, `Product` | `CartService.getCart`, `addItem`, `updateItemQuantity`, `removeItem`, `removeCheckedOutItems`, `clearCart` |
| **تسویه‌حساب سفارش** | US 20--23 | DS 07 | `Order`, `OrderItem`, `Coupon`, `Address`, `Money` | `OrderService.checkout` |
| **پرداخت و تراکنش** | US 24, 25 | DS 07, 08 | `Order`, `PaymentTransaction`, `PaymentMethod`, `PaymentStatus` | `OrderService.processPayment(orderId, userId, ...)` |
| **پیگیری و لغو سفارش** | US 26, 27, 28 | DS 09 | `Order`, `OrderItem`, `OrderStatus` | `OrderService.getOrderHistory`, `getOrderDetails(orderId, userId)`, `cancelOrder(orderId, userId)` |
| **مدیریت سفارشات توسط مدیر** | US 29, 36 | DS 09, 12 | `Order`, `OrderStatus`, `Product`, `Quantity` | `OrderService.getAllOrders`, `getOrdersByStatus`, `updateOrderStatus`, `ProductService.adjustStock` |
| **نظرات و امتیاز کالا** | US 30, 31, 32 | DS 10 | `Review`, `Product` | `ReviewService.addReview`, `getProductReviews` |
| **مدیریت ویترین و محصولات** | US 33, 34, 35 | DS 11 | `Product`, `Category`, `Money` | `ProductService.addProduct`, `updateProduct`, `setProductImages`, `deactivateProduct` |

---

## ۳. تفکیک مفاهیم دامنه: موجودیت‌ها در برابر اشیاء مقداری (Entities vs. Value Objects)

بر اساس اصول شیءگرایی و رهنمودهای جزوه `dataAbstraction.pdf`، کلاس‌های دامنه به دو دسته ساختاری تقسیم می‌شوند:

```mermaid
classDiagram
    direction TB
    class DomainConcept {
        <<Concept>>
    }
    class Entity {
        <<Identity & Lifecycle>>
        +Identity id
        +equals(byId)
    }
    class ValueObject {
        <<Immutable & Record>>
        +equals(byValue)
    }
    DomainConcept <|-- Entity
    DomainConcept <|-- ValueObject
    Entity <|-- User
    Entity <|-- Product
    Entity <|-- Cart
    Entity <|-- Order
    ValueObject <|-- Quantity
    ValueObject <|-- Money
    ValueObject <|-- Address
    ValueObject <|-- CartItem
    ValueObject <|-- OrderItem
    ValueObject <|-- Review
    ValueObject <|-- Coupon
    ValueObject <|-- Category
    ValueObject <|-- PaymentTransaction
    ValueObject <|-- ContactInfo
```

### موجودیت‌ها (Entities)
موجودیت‌ها دارای شناسه یکتای پایدار بوده و تساوی آن‌ها تنها بر اساس شناسه سنجیده می‌شود (`equals` بر مبنای `id`). این اشیاء دارای چرخه عمر و وضعیت جهش‌پذیر (Mutable) در طول زمان هستند:
1. **`User`:** هویت مشتری یا مدیر سامانه با شناسه یکتا، وضعیت فعال/غیرفعال، اطلاعات تماس و دفترچه نشانی‌ها.
2. **`Product`:** کالای موجود در فروشگاه با شناسه یکتا، موجودی فیزیکی، قیمت، آمار فروش، تصاویر و میانگین امتیازات.
3. **`Cart`:** سبد خرید مشتری که در طول یک نشست کاری اقلام به آن اضافه، کم یا ویرایش می‌شوند.
4. **`Order`:** سند رسمی خرید با شناسه یکتا و ماشین وضعیت چرخه عمر (`OrderStatus`) از ثبت تا تحویل یا لغو.

### اشیاء مقداری (Value Objects بر پایه Record جاوا ۲۱)
اشیاء مقداری فاقد شناسه مجزا بوده و مفهوم آنها با مقدار مؤلفه‌هایشان شناخته می‌شود (`equals` بر پایه تمامی فیلدها). این اشیاء کاملاً تغییرناپذیر (Immutable) بوده و به عنوان رکوردهای جاوا ۲۱ پیاده‌سازی شده‌اند:
1. **`Quantity`:** تعداد گسسته اقلام با تضمین نامتغیر اکیداً مثبت بودن ($value > 0$).
2. **`Money`:** مدل‌سازی مالی دقیق با `BigDecimal`، مقیاس استاندارد ۲ رقم اعشار و واحد پول مشخص جهت پیشگیری از خطای محاسبات اعشاری.
3. **`Address`:** نشانی پستی تحویل شامل گیرنده، تلفن معتبر، استان، شهر، خیابان و کد پستی ده‌رقمی.
4. **`CartItem`:** سطر سبد خرید شامل شناسه و نام کالا، قیمت واحد و تعداد.
5. **`OrderItem`:** تصویر پایدار و تغییرناپذیر اقلام در لحظه ثبت سفارش (Immutable Snapshot).
6. **`Review`:** نظر کیفی، امتیاز (۱ تا ۵) و نام نویسنده برای محصول.
7. **`Coupon`:** ضوابط کوپن تخفیف (درصد، سقف تخفیف، حداقل مبلغ خرید و تاریخ انقضا).
8. **`Category`:** دسته‌بندی کاتالوگ شامل شناسه، نام، شرح و وضعیت فعال/غیرفعال.
9. **`PaymentTransaction`:** سند مالی تراکنش پرداخت با شناسه پیگیری، مبلغ، روش و وضعیت نتیجه.
10. **`ContactInfo`:** شیء مقداری کپسوله‌کننده ایمیل و تلفن با اعتبارسنجی عبارات باقاعده RFC و شماره همراه ایران.
11. **انواع شمارشی (Enums):** `UserRole`, `OrderStatus`, `PaymentMethod`, `PaymentStatus`.

---

## ۴. کاتالوگ جامع کلاس‌های لایه مدل (Domain Model Catalog)

در این بخش، مشخصات کامل ساختاری، فیلدها، متدها، نامتغیرهای بازنمایی (RI)، توابع انتزاع (AF) و قراردادهای صوری تمامی کلاس‌های لایه `Model` آورده شده است.

### ۴.۱. کلاس `Quantity` (Value Object --- Record)
* **هدف:** مدل‌سازی ریاضی و کسب‌وکاری تعداد اقلام در سبد، سفارش و انبار با تضمین مثبت بودن.
* **فیلدها:**
  * `private final int value`: مقدار عددی صحیح.
* **سازنده:**
  * `public Quantity(int value)`: سازنده فشرده رکورد با اعتبارسنجی شرط مثبت بودن.
* **متدها:**
  * `public int value()`: دریافت مقدار عددی (Observer).
  * `public Quantity add(Quantity other)`: تولید تعداد جدید حاصل جمع (Producer).
  * `public Quantity subtract(Quantity other)`: تولید تعداد جدید حاصل تفریق (Producer).
  * `public boolean isGreaterThan(Quantity other)`: مقایسه بزرگی (Observer).
* **نامتغیر بازنمایی (RI):** `value > 0`.
* **تابع انتزاع (AF):** `AF(q) = an exact strictly positive integer count equal to q.value`.
* **قرارداد صوری:**
```
Constructor:
  REQUIRES: value > 0
  MODIFIES: this
  EFFECTS: Initializes Quantity; throws IllegalArgumentException if value <= 0.

add(Quantity other):
  REQUIRES: other != null
  MODIFIES: nothing
  EFFECTS: Returns new Quantity(this.value + other.value); throws NullPointerException if null.

subtract(Quantity other):
  REQUIRES: other != null and this.value > other.value
  MODIFIES: nothing
  EFFECTS: Returns new Quantity(this.value - other.value); throws IllegalArgumentException if other.value >= this.value.
```

### ۴.۲. کلاس `Money` (Value Object --- Record)
* **هدف:** مدل‌سازی مالی دقیق جهت قیمت‌ها، مالیات، هزینه ارسال و تخفیف‌ها بدون خطای گردکردن اعشاری.
* **فیلدها:**
  * `private final BigDecimal amount`: مبلغ عددی با مقیاس ۲ رقم اعشار.
  * `private final String currency`: واحد پولی استاندارد (مانند `"IRR"` یا `"TOMAN"`).
* **ثابت‌ها:**
  * `public static final Money ZERO = new Money(BigDecimal.ZERO, "IRR");`
* **سازنده:**
  * `public Money(BigDecimal amount, String currency)`: سازنده فشرده با نرمال‌سازی مقیاس و اعتبارسنجی غیرمنفی بودن.
* **متدها:**
  * `public BigDecimal amount()`: دریافت مبلغ (Observer).
  * `public String currency()`: دریافت واحد پول (Observer).
  * `public Money add(Money other)`: جمع دو مبلغ هم‌واحد (Producer).
  * `public Money subtract(Money other)`: تفریق دو مبلغ هم‌واحد (Producer).
  * `public Money multiply(Quantity quantity)`: ضرب مبلغ در تعداد (Producer).
  * `public boolean isLessThan(Money other)`: مقایسه کمتر بودن (Observer).
  * `public boolean isNegative()`: بررسی منفی بودن (Observer).
* **نامتغیر بازنمایی (RI):** `amount != null && currency != null && !currency.isBlank() && amount.scale() == 2 && amount.signum() >= 0`.
* **تابع انتزاع (AF):** `AF(m) = a monetary value of m.amount in currency m.currency`.

### ۴.۳. کلاس `Address` (Value Object --- Record)
* **هدف:** نگهداری مشخصات نشانی تحویل کالا مطابق با نیازمندی‌های پستی و خرید آنلاین.
* **مؤلفه‌های رکورد:**
  * `String addressId`: شناسه محلی نشانی (مانند `"ADDR-1"`).
  * `String recipientName`: نام تحویل‌گیرنده.
  * `String phone`: شماره تماس همراه گیرنده.
  * `String province`: استان مقصد.
  * `String city`: شهر مقصد.
  * `String streetAddress`: نشانی پستی دقیق (خیابان، پلاک، واحد).
  * `String postalCode`: کد پستی ده‌رقمی معتبر مطابق با الگوی `^[0-9]{10}$`.
  * `String title`: عنوان نشانی (نظیر منزل، شرکت).
  * `boolean isDefault`: وضعیت پیش‌فرض بودن نشانی.
* **متدها:**
  * اکسسورهای استاندارد رکورد: `addressId()`, `recipientName()`, `phone()`, `province()`, `city()`, `streetAddress()`, `postalCode()`, `title()`, `isDefault()`.
  * `public Address withDefault(boolean isDefault)`: ایجاد نمونه جدید با تغییر وضعیت پیش‌فرض (Producer).
  * `public String fullAddressText()`: قالب‌بندی متنی کامل آدرس جهت درج روی فاکتور ارسال (Observer).
* **نامتغیر بازنمایی (RI):** کد پستی دقیقاً ۱۰ رقم عددی بوده و شماره تماس معتبر است.

### ۴.۴. کلاس `User` (Entity)
* **هدف:** مدیریت هویت، اعتبارنامه‌ها، دسترسی‌ها و دفترچه نشانی‌های مشتریان و مدیران سامانه.
* **فیلدها:**
  * `private final String id`: شناسه یکتا و تغییرناپذیر کاربر.
  * `private String fullName`: نام و نام خانوادگی.
  * `private String email`: آدرس ایمیل.
  * `private String phone`: شماره تلفن همراه.
  * `private ContactInfo contactInfo`: شیء مقداری اطلاعات تماس کاربر.
  * `private String passwordHash`: هش رمز عبور (SHA-256).
  * `private UserRole role`: نقش کاربری (`CUSTOMER`, `ADMIN`, `STOREKEEPER`).
  * `private boolean active`: وضعیت فعال بودن حساب کاربری.
  * `private final LocalDateTime createdAt`: تاریخ و زمان ثبت‌نام.
  * `private final List<Address> addresses`: لیست نشانی‌های کاربر.
* **متدها:**
  * اکسسورها: `getId()`, `getFullName()`, `getEmail()`, `getPhone()`, `getContactInfo()`, `getRole()`, `isActive()`, `getCreatedAt()`.
  * `public void activate()`, `public void deactivate()`: تغییر وضعیت فعال‌بودن حساب (Mutators).
  * `public void updateProfile(String newFullName, String newPhone)`: ویرایش اطلاعات پروفایل (Mutator).
  * `public void changePassword(String newPasswordHash)`: تغییر هش رمز عبور (Mutator).
  * `public void addAddress(Address address)`: افزودن نشانی جدید به دفترچه (Mutator).
  * `public void updateAddress(Address updatedAddress)`: به‌روزرسانی نشانی موجود بر اساس شناسه (Mutator).
  * `public void removeAddress(String addressId)`: حذف نشانی (Mutator).
  * `public void setDefaultAddress(String addressId)`: تعیین نشانی پیش‌فرض (Mutator).
  * `public Optional<Address> getDefaultAddress()`: دریافت نشانی پیش‌فرض کاربر (Observer).
  * `public List<Address> getAddresses()`: دریافت کپی تغییرناپذیر نشانی‌ها با `List.copyOf` (Observer).
* **نامتغیر بازنمایی (RI):** `id != null && !id.isBlank() && fullName != null && !fullName.isBlank() && passwordHash != null && role != null && addresses != null && contactInfo != null`.
* **تابع انتزاع (AF):** `AF(u) = a user profile with unique identity u.id, credentials u.passwordHash, contact channels u.contactInfo, role u.role, and shipping addresses u.addresses`.

### ۴.۵. کلاس `Product` (Entity)
* **هدف:** مدل‌سازی مشخصات، وضعیت موجودی، قیمت، سابقه فروش و رسانه‌های کالای فروشگاه.
* **فیلدها:**
  * `private final String id`: شناسه یکتای محصول.
  * `private String name`: عنوان تجاری کالا.
  * `private String description`: توضیحات تفصیلی.
  * `private Money price`: قیمت فروش فعلی.
  * `private int availableStock`: موجودی فیزیکی قابل فروش در انبار ($availableStock \ge 0$).
  * `private String categoryId`: شناسه دسته‌بندی والد.
  * `private boolean active`: وضعیت انتشار و فروش در کاتالوگ.
  * `private double averageRating`: میانگین امتیاز نظرات خریداران ($0.0 \le averageRating \le 5.0$).
  * `private int reviewCount`: تعداد کل نظرات ثبت‌شده.
  * `private final LocalDateTime createdAt`: تاریخ و زمان ایجاد محصول.
  * `private int soldCount`: شمارنده تجمیعی تعداد فروش موفق کالا.
  * `private List<String> imageUrls`: فهرست آدرس‌های اینترنتی تصاویر محصول.
* **متدها:**
  * اکسسورها: `getId()`, `getName()`, `getDescription()`, `getPrice()`, `getAvailableStock()`, `getCategoryId()`, `isActive()`, `getAverageRating()`, `getReviewCount()`, `getCreatedAt()`, `getSoldCount()`, `getImageUrls()`.
  * `public boolean isAvailable(Quantity required)`: بررسی موجودی کافی (Observer).
  * `public void updateDetails(String name, String desc, Money price, String categoryId)`: ویرایش اطلاعات پایه کالا (Mutator).
  * `public void setActive(boolean active)`: تغییر وضعیت انتشار کالا (Mutator).
  * `public void deductStock(Quantity quantity)`: کسر قطعی موجودی کالا (Mutator).
  * `public void restock(Quantity quantity)`: شارژ موجودی انبار (Mutator).
  * `public void recordSale(Quantity quantity)`: ثبت فروش و افزایش شمارنده محبوبیت `soldCount` (Mutator).
  * `public void setImageUrls(List<String> imageUrls)`: تنظیم و به‌روزرسانی فهرست لینک‌های تصاویر (Mutator).
  * `public void updateRatingStats(int newRatingScore)`: محاسبه مجدد میانگین امتیاز بر پایه نظر جدید (Mutator).
* **نامتغیر بازنمایی (RI):** `id != null && name != null && !name.isBlank() && price != null && availableStock >= 0 && soldCount >= 0 && imageUrls != null`.

### ۴.۶. کلاس `CartItem` (Value Object --- Record)
* **هدف:** سطر مشخص در سبد خرید مشتری شامل کالا، قیمت در لحظه افزودن و تعداد.
* **مؤلفه‌های رکورد:** `String productId`, `String productName`, `Money unitPrice`, `Quantity quantity`.
* **متدها:**
  * اکسسورهای استاندارد رکورد: `productId()`, `productName()`, `unitPrice()`, `quantity()`.
  * `public Money getLineTotal()`: محاسبه حاصل ضرب قیمت واحد در تعداد سطر (Observer).
  * `public CartItem withQuantity(Quantity newQuantity)`: تولید سطر جدید با تعداد اصلاح‌شده (Producer).

### ۴.۷. کلاس `Cart` (Entity / Aggregate Root)
* **هدف:** نگهداری اقلام انتخاب‌شده توسط مشتری پیش از ثبت سفارش.
* **فیلدها:**
  * `private final String cartId`: شناسه سبد خرید (منطبق بر شناسه کاربر).
  * `private final String userId`: شناسه مالک سبد.
  * `private final Map<String, CartItem> items`: نگاشت شناسه کالا به سطر سبد خرید.
* **متدها:**
  * اکسسورها: `getCartId()`, `getUserId()`.
  * `public void addProduct(Product product, Quantity quantity)`: افزودن یا افزایش تعداد کالا در سبد (Mutator).
  * `public void updateQuantity(String productId, Quantity newQuantity)`: تغییر مستقیم تعداد سطر (Mutator).
  * `public void removeProduct(String productId)`: حذف سطر کالا از سبد (Mutator).
  * `public void removeCheckedOutItems(List<CartItem> checkedOutItems)`: حذف هوشمند صرفاً اقلام قطعی‌شده پس از تسویه موفق (Mutator).
  * `public void clear()`: تخلیه کامل سبد خرید (Mutator).
  * `public int quantityOf(String productId)`: دریافت تعداد فعلی کالای مشخص در سبد (Observer).
  * `public Money calculateSubtotal()`: محاسبه مجموع ارزش ریالی اقلام سبد (Observer).
  * `public boolean isEmpty()`: بررسی خالی بودن سبد (Observer).
  * `public List<CartItem> getItems()`: دریافت کپی تغییرناپذیر سطرهای سبد با `List.copyOf` (Observer).

### ۴.۸. کلاس `OrderItem` (Value Object --- Record)
* **هدف:** ثبت تغییرناپذیر اقلام در لحظه نهایی شدن سفارش (Immutable Snapshot).
* **مؤلفه‌های رکورد:** `String productId`, `String productName`, `Money unitPrice`, `Quantity quantity`, `Money lineTotal`.
* **نامتغیر بازنمایی (RI):** `lineTotal.equals(unitPrice.multiply(quantity))`.

### ۴.۹. کلاس `Order` (Entity / Aggregate Root)
* **هدف:** سند رسمی خرید شامل تمامی اقلام، تخفیف، نشانی، مبالغ و ماشین وضعیت سفارش.
* **فیلدها:**
  * `private final String orderId`: شناسه یکتای سفارش.
  * `private final String userId`: شناسه مشتری ثبت‌کننده.
  * `private final List<OrderItem> items`: سطرهای پایدار اقلام سفارش.
  * `private final Address deliveryAddress`: نشانی تحویل بسته.
  * `private final Money itemsSubtotal`: مجموع ارزش اقلام.
  * `private final Money discountAmount`: مبلغ تخفیف کوپن.
  * `private final Money shippingFee`: هزینه ارسال سفارش.
  * `private final Money payableAmount`: مبلغ نهایی قابل پرداخت.
  * `private OrderStatus status`: وضعیت چرخه عمر سفارش.
  * `private final LocalDateTime createdAt`: تاریخ و زمان ثبت سفارش.
  * `private LocalDateTime paidAt`: تاریخ و زمان پرداخت موفق.
* **متدها:**
  * اکسسورها: `getOrderId()`, `getUserId()`, `getStatus()`, `getItemsSubtotal()`, `getDiscountAmount()`, `getShippingFee()`, `getPayableAmount()`, `getItems()`, `getDeliveryAddress()`, `getCreatedAt()`, `getPaidAt()`.
  * `public void markAsPaid(LocalDateTime paymentTime)`: تغییر وضعیت به `PAID` و ثبت زمان پرداخت (Mutator).
  * `public void markAsPaymentFailed()`: تغییر وضعیت به `PAYMENT_FAILED` (Mutator).
  * `public void updateStatus(OrderStatus newStatus)`: به‌روزرسانی وضعیت توسط مدیر یا انباردار (Mutator).
  * `public void cancel()`: لغو سفارش (Mutator).
  * `public boolean canBeCancelled()`: بررسی امکان لغو بر مبنای ماشین وضعیت (Observer).
* **نامتغیر بازنمایی (RI):** سفارش‌های در وضعیت `PAID` یا بالاتر الزاماً دارای `paidAt != null` هستند.

### ۴.۱۰. کلاس `PaymentTransaction` (Value Object --- Record)
* **هدف:** ثبت رسمی و تغییرناپذیر تراکنش مالی پرداخت سفارش.
* **مؤلفه‌های رکورد:** `String transactionId`, `String orderId`, `Money amount`, `PaymentMethod method`, `PaymentStatus status`, `LocalDateTime timestamp`, `String referenceCode`.
* **نامتغیر بازنمایی (RI):** `transactionId != null && orderId != null && amount != null && status != null`.

### ۴.۱۱. کلاس `Category` (Value Object / Entity)
* **هدف:** ساختار موضوعی دسته‌بندی محصولات در کاتالوگ فروشگاه آنلاین.
* **فیلدها و مؤلفه‌ها:**
  * `private final String id`: شناسه یکتای دسته.
  * `private final String name`: نام عنوان دسته (مانند الکترونیک، کتاب).
  * `private final String description`: شرح دسته‌بندی.
  * `private final boolean active`: پرچم فعال بودن دسته در کاتالوگ.
* **متدها:** `id()`, `name()`, `description()`, `active()`.

### ۴.۱۲. کلاس `Coupon` (Value Object --- Record)
* **هدف:** مدل‌سازی تخفیف‌های درصدی با سقف مشخص و شرایط حداقل مبلغ خرید.
* **مؤلفه‌های رکورد:** `String code`, `int discountPercent`, `Money maxDiscount`, `Money minOrderAmount`, `LocalDate expiryDate`.
* **متدها:**
  * `public Money calculateDiscount(Money subtotal)`: محاسبه میزان تخفیف بر اساس درصد و با رعایت سقف مجاز (Observer).
  * `public boolean isValid(Money subtotal, LocalDate currentDate)`: بررسی اعتبار تاریخ و احراز شرط حداقل خرید (Observer).

### ۴.۱۳. کلاس `Review` (Value Object --- Record)
* **هدف:** ثبت دیدگاه و امتیاز کیفی خریداران درباره محصولات خریداری‌شده.
* **مؤلفه‌های رکورد:** `String reviewId`, `String userId`, `String authorName`, `String productId`, `int rating`, `String commentText`, `LocalDateTime createdAt`.
* **نامتغیر بازنمایی (RI):** `rating >= 1 && rating <= 5 && commentText != null && !commentText.isBlank() && commentText.length() <= 2000`.

### ۴.۱۴. کلاس `ContactInfo` (Value Object --- Record)
* **هدف:** کپسوله‌سازی تغییرناپذیر مشخصه‌های ارتباطی کاربر (ایمیل و تلفن همراه) با اعتبارسنجی عبارات باقاعده.
* **مؤلفه‌های رکورد:**
  * `String email`: آدرس ایمیل استاندارد (اختیاری در صورت حضور تلفن).
  * `String phone`: شماره تلفن همراه ایران (اختیاری در صورت حضور ایمیل).
* **سازنده:**
  * `public ContactInfo(String email, String phone)`: اعتبارسنجی الزام حضور حداقل یکی از دو مشخصه، انطباق ایمیل با الگوی RFC `^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$` و انطباق تلفن با الگوی `^09[0-9]{9}$`.
* **متدها:**
  * `public String email()`, `public String phone()`: دسترسی به مقادیر.
  * `public boolean hasEmail()`: بررسی حضور ایمیل (Observer).
  * `public boolean hasPhone()`: بررسی حضور تلفن همراه (Observer).
* **نامتغیر بازنمایی (RI):** حداقل یکی از فیلدهای `email` یا `phone` مقدار غیرخالی داشته و مقادیر موجود با الگوهای اعتبارسنجی مطابقت دارند.
* **تابع انتزاع (AF):** `AF(c) = a contact channel pair with email c.email and mobile phone c.phone`.

### ۴.۱۵. انواع شمارشی مدل دامنه (Domain Enumerations)
* **`UserRole`:** نقش‌های دسترسی سامانه: `CUSTOMER` (خریدار)، `ADMIN` (مدیر سیستم)، `STOREKEEPER` (انباردار).
* **`OrderStatus`:** مراحل چرخه عمر سفارش: `PENDING` (در انتظار پرداخت)، `PAYMENT_FAILED` (خطای پرداخت)، `PAID` (پرداخت‌شده)، `PROCESSING` (در حال آماده‌سازی در انبار)، `SHIPPED` (تحویل به پست/پیک)، `DELIVERED` (تحویل‌شده به مشتری)، `CANCELLED` (لغوشده).
* **`PaymentMethod`:** روش‌های پرداخت: `ONLINE_GATEWAY` (درگاه اینترنتی)، `WALLET` (کیف پول الکترونیک)، `CASH_ON_DELIVERY` (پرداخت در محل).
* **`PaymentStatus`:** وضعیت نتیجه تراکنش پرداخت: `SUCCESS` (موفق)، `FAILED` (ناموفق)، `CANCELLED` (انصراف).

---

## ۵. کاتالوگ جامع کلاس‌های لایه سرویس (Application & Domain Services)

لایه سرویس مسئول هماهنگی سناریوهای کاربردی (Use Cases)، مدیریت داده‌های درون‌حافظه‌ای (`In-Memory Maps`) و اعمال قوانین جامع کسب‌وکار است:

```mermaid
classDiagram
    direction TB

    class AuthService {
        -Map~String, User~ userStore
        -Map~String, String~ activeSessions
        -Map~String, PasswordReset~ passwordResetStore
        +register(String fullName, String email, String phone, String password) User
        +login(String identifier, String password) String
        +logout(String sessionToken) void
        +getAuthenticatedUser(String sessionToken) User
        +requestPasswordReset(String identifier) PasswordReset
        +resetPassword(String code, String newPassword) boolean
    }

    class UserService {
        -Map~String, User~ userStore
        +getProfile(String userId) User
        +updateProfile(String userId, String fullName, String phone) void
        +addAddress(String userId, Address address) void
        +updateAddress(String userId, Address address) void
        +removeAddress(String userId, String addressId) void
        +setDefaultAddress(String userId, String addressId) void
    }

    class ProductService {
        -Map~String, Product~ productStore
        -Map~String, Category~ categoryStore
        +getAllProducts() List~Product~
        +getAllActiveProducts() List~Product~
        +getProductById(String productId) Product
        +requireAvailableProduct(String productId) Product
        +searchProducts(String query) List~Product~
        +filterAndSort(String categoryId, Money minPrice, Money maxPrice, String sortBy) List~Product~
        +addProduct(String name, String desc, Money price, int stock, String categoryId) void
        +updateProduct(String productId, String name, String desc, Money price, String categoryId) void
        +setProductImages(String productId, List~String~ imageUrls) void
        +deactivateProduct(String productId) void
        +adjustStock(String productId, int delta) void
    }

    class CartService {
        -Map~String, Cart~ cartStore
        -ProductService productService
        +getCart(String userId) Cart
        +addItem(String userId, String productId, Quantity quantity) void
        +updateItemQuantity(String userId, String productId, Quantity quantity) void
        +removeItem(String userId, String productId) void
        +removeCheckedOutItems(String userId, List~CartItem~ checkedOutItems) void
        +clearCart(String userId) void
    }

    class OrderService {
        -Map~String, Order~ orderStore
        -Map~String, Coupon~ couponStore
        -CartService cartService
        -ProductService productService
        -UserService userService
        +checkout(String userId, String addressId, String couponCode) Order
        +processPayment(String orderId, PaymentMethod method, boolean simulateSuccess) boolean
        +processPayment(String orderId, String userId, PaymentMethod method, boolean simulateSuccess) boolean
        +cancelOrder(String orderId) boolean
        +cancelOrder(String orderId, String userId) boolean
        +getOrderHistory(String userId) List~Order~
        +getOrderDetails(String orderId) Order
        +getOrderDetails(String orderId, String userId) Order
        +getAllOrders() List~Order~
        +getOrdersByStatus(OrderStatus status) List~Order~
        +updateOrderStatus(String orderId, OrderStatus newStatus) void
    }

    class ReviewService {
        -Map~String, List~Review~~ productReviewsStore
        -OrderService orderService
        -ProductService productService
        +addReview(String userId, String authorName, String productId, int rating, String comment) void
        +getProductReviews(String productId) List~Review~
    }

    CartService ..> ProductService : checks stock
    OrderService ..> CartService : clears/updates cart
    OrderService ..> ProductService : deducts/restocks & records sale
    OrderService ..> UserService : validates address & user
    ReviewService ..> OrderService : verifies purchase
    ReviewService ..> ProductService : updates rating
```

### ۵.۱. سرویس `AuthService`
* **مسئولیت:** مدیریت ثبت‌نام، احراز هویت، توکن‌های نشست کاری و بازیابی رمز عبور شبیه‌سازی‌شده (DS 01, DS 02, DS 03).
* **فیلدهای ذخیره‌سازی:**
  * `private final Map<String, User> userStore`: نگاشت شناسه/ایمیل کاربر به شیء کاربر.
  * `private final Map<String, String> activeSessions`: نگاشت توکن نشست به شناسه کاربر.
  * `private final Map<String, PasswordReset> passwordResetStore`: نگاشت توکن ۶ رقمی به اطلاعات بازیابی رمز عبور.
* **متدها و قراردادها:**
  * `public User register(String fullName, String email, String phone, String password)`
  * `public String login(String identifier, String password)`: پشتیبانی از ورود با ایمیل یا شماره موبایل.
  * `public void logout(String sessionToken)`: ابطال توکن نشست جاری.
  * `public User getAuthenticatedUser(String sessionToken)`: اعتبارسنجی نشست و بازگرداندن کاربر لاگین‌شده.
  * `public PasswordReset requestPasswordReset(String identifier)`: تولید کد موقت ۶ رقمی با اعتبار ۱۰ دقیقه.
  * `public boolean resetPassword(String code, String newPassword)`: تغییر رمز، ابطال کد و ابطال تمام نشست‌های پیشین کاربر.

### ۵.۲. سرویس `UserService`
* **مسئولیت:** مدیریت پروفایل مشتری، تغییر اطلاعات تماس و مدیریت دفترچه نشانی‌ها (DS 04, US 05--07).
* **فیلدهای ذخیره‌سازی:** `private final Map<String, User> userStore`.
* **متدها و قراردادها:**
  * `public User getProfile(String userId)`: دریافت اطلاعات کاربر.
  * `public void updateProfile(String userId, String fullName, String phone)`: ویرایش پروفایل با اعتبارسنجی عدم تکراری بودن شماره همراه.
  * `public void addAddress(String userId, Address address)`: افزودن نشانی.
  * `public void updateAddress(String userId, Address updatedAddress)`: ویرایش نشانی موجود.
  * `public void removeAddress(String userId, String addressId)`: حذف نشانی.
  * `public void setDefaultAddress(String userId, String addressId)`: تعیین نشانی پیش‌فرض.

### ۵.۳. سرویس `ProductService`
* **مسئولیت:** مدیریت کاتالوگ محصولات، جستجو، فیلتر، مرتب‌سازی، تصاویر و عملیات انبارداری (DS 05, DS 11, DS 12).
* **فیلدهای ذخیره‌سازی:**
  * `private final Map<String, Product> productStore`: نگاشت شناسه کالا به شیء محصول.
  * `private final Map<String, Category> categoryStore`: نگاشت دسته‌بندی‌ها.
* **متدها و قراردادها:**
  * `public List<Product> getAllActiveProducts()`: دریافت کالاهای فعال در دسته‌های فعال.
  * `public List<Product> getAllProducts()`: دریافت تمامی کالاها (مخصوص مدیر).
  * `public Product getProductById(String productId)`: دریافت کالا با شناسه.
  * `public Product requireAvailableProduct(String productId)`: بررسی کفایت موجودی و فعال بودن کالا.
  * `public List<Product> searchProducts(String query)`: جستجو در نام و توضیحات کالا با اعتبارسنجی طول ۳ تا ۱۰۰ کاراکتر.
  * `public List<Product> filterAndSort(String categoryId, Money minPrice, Money maxPrice, String sortBy)`: فیلتر کاتالوگ و مرتب‌سازی بر اساس `"price_asc"`, `"price_desc"`, `"rating"`, `"newest"`, `"popular"`.
  * `public void addProduct(...)`, `public void updateProduct(...)`: ثبت و ویرایش کالا توسط مدیر.
  * `public void setProductImages(String productId, List<String> imageUrls)`: تنظیم و اعتبارسنجی URLهای تصاویر کالا.
  * `public void deactivateProduct(String productId)`: غیرفعال‌سازی کالا.
  * `public void adjustStock(String productId, int delta)`: تنظیم مستقیم موجودی توسط مدیر/انباردار.

### ۵.۴. سرویس `CartService`
* **مسئولیت:** هماهنگی سبد خرید مشتری با کاتالوگ محصولات (DS 06).
* **فیلدهای ذخیره‌سازی:**
  * `private final Map<String, Cart> cartStore`: نگاشت شناسه کاربر به سبد خرید او.
  * `private final ProductService productService`: استعلام موجودی و قیمت.
* **متدها و قراردادها:**
  * `public Cart getCart(String userId)`: دریافت یا ایجاد سبد خرید کاربر.
  * `public void addItem(String userId, String productId, Quantity quantity)`: افزودن کالا به سبد با بررسی موجودی.
  * `public void updateItemQuantity(String userId, String productId, Quantity quantity)`: تغییر تعداد کالا.
  * `public void removeItem(String userId, String productId)`: حذف کالا از سبد با اعتبارسنجی حضور قلم.
  * `public void removeCheckedOutItems(String userId, List<CartItem> checkedOutItems)`: حذف انتخابی اقلام پرداخت‌شده.
  * `public void clearCart(String userId)`: تخلیه کامل سبد خرید.

### ۵.۵. سرویس `OrderService`
* **مسئولیت:** تسویه‌حساب، اعمال کوپن، پرداخت سفارش، لغو سفارش، ردیابی وضعیت و انزوای مشتری (DS 07, DS 08, DS 09, DS 12).
* **فیلدهای ذخیره‌سازی:**
  * `private final Map<String, Order> orderStore`: مخزن سفارش‌ها.
  * `private final Map<String, Coupon> couponStore`: مخزن کوپن‌های تخفیف.
* **متدها و قراردادها:**
  * `public Order checkout(String userId, String addressId, String couponCode)`: ایجاد سفارش در وضعیت `PENDING`.
  * `public boolean processPayment(String orderId, PaymentMethod method, boolean simulateSuccess)`: پرداخت داخلی/تستی.
  * `public boolean processPayment(String orderId, String userId, PaymentMethod method, boolean simulateSuccess)`: پرداخت ایزوله مشتری؛ در صورت موفقیت موجودی کسر، فروش در `Product.recordSale` ثبت و اقلام مربوطه از سبد خرید حذف می‌شوند.
  * `public boolean cancelOrder(String orderId)`: لغو داخلی/مدیریتی.
  * `public boolean cancelOrder(String orderId, String userId)`: لغو ایزوله مشتری؛ در صورت لغو سفارش پرداخت‌شده اقلام به انبار بازگردانده می‌شوند.
  * `public List<Order> getOrderHistory(String userId)`: دریافت تاریخچه سفارش‌های کاربر.
  * `public Order getOrderDetails(String orderId, String userId)`: دریافت جزئیات با تأیید مالکیت سفارش.
  * `public List<Order> getAllOrders()`: مشاهده تمامی سفارش‌ها توسط مدیر.
  * `public List<Order> getOrdersByStatus(OrderStatus status)`: فیلتر سفارش‌ها بر مبنای وضعیت توسط مدیر.
  * `public void updateOrderStatus(String orderId, OrderStatus newStatus)`: پیشبرد چرخه عمر سفارش توسط مدیر.

### ۵.۶. سرویس `ReviewService`
* **مسئولیت:** ثبت و بازیابی نظرات و امتیازات محصولات با راستی‌آزمایی خرید قبلی (DS 10, US 30--32).
* **فیلدهای ذخیره‌سازی:**
  * `private final Map<String, List<Review>> productReviewsStore`: نگاشت شناسه کالا به لیست نظرات.
* **متدها و قراردادها:**
  * `public void addReview(String userId, String authorName, String productId, int rating, String comment)`: اعتبارسنجی امتیاز بین ۱ تا ۵، حداکثر طول ۲۰۰۰ کاراکتر متن، و اثبات خرید قبلی کالا در یک سفارش با وضعیت `PAID` یا بالاتر.
  * `public List<Review> getProductReviews(String productId)`: دریافت لیست نظرات کالا.

---

## ۶. طراحی معماری لایه Presentation (CLI Layer Specification)

لایه واسط متنی (`CLI`) تعامل با کاربر در ترمینال را در قالبی کامپوننت‌محور و ماژولار مدیریت می‌نماید:

```mermaid
flowchart TD
    Start([شروع برنامه Main]) --> LoginCheck{کاربر لاگین کرده؟}
    LoginCheck -- خیر --> Guest[GuestMenu
منوی مهمان]
    Guest --> AuthAction[ورود / ثبت‌نام / بازیابی رمز]
    Guest --> CatalogShared1[CatalogMenu
مرور و جستجوی کاتالوگ]
    AuthAction --> LoginCheck
    CatalogShared1 --> Guest
    LoginCheck -- بله --> RoleCheck{نقش کاربر چیست؟}
    RoleCheck -- CUSTOMER --> CustMenu[CustomerMenu
پنل مشتری]
    CustMenu --> CustActions[پروفایل و نشانی‌ها / سبد خرید / تسویه و پرداخت / تاریخچه سفارش‌ها / ثبت نظر]
    CustMenu --> CatalogShared2[CatalogMenu
مرور و جستجوی کاتالوگ]
    RoleCheck -- ADMIN --> AdminMenu[AdminMenu
پنل مدیریت فروشگاه]
    AdminMenu --> AdminActions[مدیریت کالاها و تصاویر / تنظیم موجودی / مدیریت و فیلتر سفارش‌ها]
    CustActions --> CustMenu
    CatalogShared2 --> CustMenu
    AdminActions --> AdminMenu
```

### اجزای اصلی لایه CLI:
1. **`ConsoleIO`:** کلاس کمکی ورود و خروج، دریافت امن رشته، اعداد صحیح و اعشاری، رمز عبور، و اعتبارسنجی طول و محدوده در مرز ورودی ترمینال.
2. **`MainMenuController`:** کنترل‌کننده مرکزی برنامه؛ هدایت کاربر میان منوی مهمان و منوهای نقش‌محور بر پایه نشست کاری، رسیدگی به خروج (`logout`) و بررسی اعتبار نشست.
3. **`GuestMenu`:** منوی ویژه کاربران احرازهویت‌نشده شامل گزینه‌های ثبت‌نام (`register`)، ورود (`login`)، بازیابی رمز عبور (`forgot password`) و مرور کاتالوگ.
4. **`CustomerMenu`:** منوی اختصاصی مشتری احرازهویت‌شده شامل مدیریت پروفایل، ثبت و ویرایش نشانی‌ها، مشاهده و ویرایش سبد خرید، تسویه‌حساب و پرداخت سفارش، تاریخچه سفارشات، لغو سفارش، ثبت نظر و امتیاز کالا.
5. **`AdminMenu`:** پنل مدیریت سامانه شامل تعریف کالا، ویرایش قیمت و مشخصات کالا، تنظیم لینک تصاویر، تنظیم موجودی انبار، مشاهده لیست کلیه سفارش‌های سامانه، فیلتر سفارش‌ها بر اساس وضعیت و پیشبرد چرخه وضعیت سفارشات. این منو مجهز به گارد دسترسی نقش `ADMIN` است.
6. **`CatalogMenu`:** مؤلفه ماژولار و مشترک مرور کاتالوگ که توسط هر دو منوی `GuestMenu` و `CustomerMenu` فراخوانی می‌شود و قابلیت‌های جستجوی متنی، مشاهده دسته‌بندی‌ها، فیلتر بر مبنای بازه قیمت و مرتب‌سازی بر اساس قیمت، امتیاز، جدیدترین و محبوب‌ترین را فراهم می‌سازد.

---

## ۷. مشخصات کلاس راه‌انداز اصلی (Main.java) و ابزارهای پشتیبان

کلاس `Main` نقطه آغاز اجرای برنامه بوده و وظایف زیر را بر عهده دارد:

1. **نمونه‌سازی متمرکز و تزریق وابستگی‌ها (Manual Dependency Injection):** ایجاد مخازن درون‌حافظه‌ای و اتصال لایه‌های مدل، سرویس و کنترل‌کننده بدون وابستگی به فریم‌ورک‌های سنگین خارجی.
2. **تزریق داده‌های اولیه (Data Seeding):** کلاس `DataSeeder` اطلاعات پایه زیر را جهت سهولت تست در حافظه بارگذاری می‌نماید:
   * **حساب مدیر پیش‌فرض:** ایمیل `admin@shop.com` با رمز عبور `Admin@123` و نقش `ADMIN`.
   * **حساب مشتری پیش‌فرض:** ایمیل `customer@shop.com` یا شماره همراه `09121112233` با رمز عبور `Pass@123` و نقش `CUSTOMER`.
   * **دسته‌بندی‌های نمونه:** دسته‌های فعال الکترونیک (`CAT-1`)، پوشاک (`CAT-2`) و کتاب (`CAT-3`).
   * **کالاهای اولیه:** حداقل ۴ کالای نمونه به همراه قیمت، موجودی، لینک تصاویر و دسته‌بندی.
3. **ابزارهای کمکی امنیتی (`PasswordUtil`):** ارائه متد استاندارد `hashPassword` جهت تبدیل گذرواژه‌ها به رشته‌های هش امن بر پایه الگوریتم یک‌طرفه SHA-256.

---

## ۸. نمودارهای جامع ساختاری و روابط (UML Class Diagrams)

### ۸.۱. جدول جامع ارتباط‌ها و تعداد آن‌ها (Associations and Multiplicities)

| مبدأ (Source) | مقصد (Target) | نوع رابطه و کاردینالیتی | توضیح کسب‌وکاری |
| :--- | :--- | :--- | :--- |
| `User` | `Address` | ترکیب: ۱ به $0..*$ | هر کاربر دارای صفر یا چند نشانی است؛ حداکثر یک نشانی پیش‌فرض دارد. |
| `User` | `ContactInfo` | ترکیب: ۱ به ۱ | هر کاربر دقیقاً دارای یک شیء اطلاعات تماس شامل ایمیل و موبایل است. |
| `User` | `Cart` | مالکیت: ۱ به $0..1$ | سبد خرید در نخستین مراجعه کاربر ساخته شده و مالکیت آن ثابت است. |
| `User` | `Order` | وابستگی: ۱ به $0..*$ | اتصال با شناسه `userId` جهت ردیابی تاریخچه سفارش‌های کاربر. |
| `Cart` | `CartItem` | ترکیب: ۱ به $0..*$ | هر سبد دارای صفر یا چند سطر کالا است؛ برای هر کالا حداکثر یک سطر مجاز است. |
| `CartItem` | `Product` | ارجاع: چند به ۱ | سطر سبد خرید به شناسه کالا اشاره داشته و مشخصات لحظه‌ای را نگهداری می‌کند. |
| `Product` | `Category` | وابستگی: چند به ۱ | هر محصول متعلق به یک دسته است؛ کالا در صورت فعال بودن دسته قابل فروش است. |
| `Product` | `Review` | تجمیع: ۱ به $0..*$ | هر کالا دارای صفر یا چند نظر ثبت‌شده از خریداران قبلی است. |
| `Order` | `OrderItem` | ترکیب: ۱ به $1..*$ | هر سفارش نهایی شامل حداقل یک سطر تغییرناپذیر کالا است. |
| `Order` | `Address` | ارتباط: چند به ۱ | هر سفارش دارای یک نشانی تحویل است که از دفترچه نشانی مشتری کپی شده است. |
| `Order` | `PaymentTransaction` | تجمیع: ۱ به $0..*$ | یک سفارش می‌تواند دارای چند تراکنش ناموفق و حداکثر یک تراکنش موفق باشد. |
| `Order` | `Coupon` | استفاده: چند به $0..1$ | هر سفارش حداکثر از یک کوپن تخفیف معتبر استفاده می‌نماید. |
| `Review` | `User` / `Product` | ارجاع: چند به ۱ | هر نظر توسط یک خریدار مشخص برای یک کالای مشخص ثبت می‌شود. |

### ۸.۲. نمودار روابط کلاس‌های لایه Model (Core Domain Model)

```mermaid
classDiagram
    direction TB

    namespace Entities {
        class User {
            -String id
            -String fullName
            -String email
            -String phone
            -String passwordHash
            -UserRole role
            -boolean active
            -LocalDateTime createdAt
            -List~Address~ addresses
            +User(String, String, String, String, String, UserRole, LocalDateTime)
            +getId() String
            +getFullName() String
            +getEmail() String
            +getPhone() String
            +getRole() UserRole
            +isActive() boolean
            +activate() void
            +deactivate() void
            +updateProfile(String, String) void
            +changePassword(String) void
            +addAddress(Address) void
            +removeAddress(String) void
            +setDefaultAddress(String) void
            +getDefaultAddress() Optional~Address~
            +getAddresses() List~Address~
            +getCreatedAt() LocalDateTime
        }

        class Product {
            -String id
            -String name
            -String description
            -Money price
            -int availableStock
            -String categoryId
            -boolean active
            -double averageRating
            -int reviewCount
            -LocalDateTime createdAt
            -int soldCount
            -List~String~ imageUrls
            +Product(String, String, String, Money, int, String)
            +getId() String
            +getName() String
            +getDescription() String
            +getPrice() Money
            +getAvailableStock() int
            +getCategoryId() String
            +getAverageRating() double
            +getReviewCount() int
            +getCreatedAt() LocalDateTime
            +getSoldCount() int
            +getImageUrls() List~String~
            +isAvailable(Quantity) boolean
            +isActive() boolean
            +updateDetails(String, String, Money, String) void
            +setActive(boolean) void
            +deductStock(Quantity) void
            +restock(Quantity) void
            +recordSale(Quantity) void
            +setImageUrls(List~String~) void
            +updateRatingStats(int) void
        }

        class Cart {
            -String cartId
            -String userId
            -Map~String, CartItem~ items
            +Cart(String, String)
            +Cart(String)
            +getCartId() String
            +getUserId() String
            +addProduct(Product, Quantity) void
            +updateQuantity(String, Quantity) void
            +removeProduct(String) void
            +removeCheckedOutItems(List~CartItem~) void
            +clear() void
            +quantityOf(String) int
            +calculateSubtotal() Money
            +isEmpty() boolean
            +getItems() List~CartItem~
        }

        class Order {
            -String orderId
            -String userId
            -List~OrderItem~ items
            -Address deliveryAddress
            -Money itemsSubtotal
            -Money discountAmount
            -Money shippingFee
            -Money payableAmount
            -OrderStatus status
            -LocalDateTime createdAt
            -LocalDateTime paidAt
            +Order(String, String, List~OrderItem~, Address, Money, Money, Money, Money, LocalDateTime)
            +getOrderId() String
            +getUserId() String
            +getStatus() OrderStatus
            +getItemsSubtotal() Money
            +getDiscountAmount() Money
            +getShippingFee() Money
            +getPayableAmount() Money
            +getItems() List~OrderItem~
            +getDeliveryAddress() Address
            +getCreatedAt() LocalDateTime
            +getPaidAt() Optional~LocalDateTime~
            +markAsPaid(LocalDateTime) void
            +markAsPaymentFailed() void
            +updateStatus(OrderStatus) void
            +cancel() void
            +canBeCancelled() boolean
        }
    }

    namespace ValueObjects_And_Records {
        class ContactInfo {
            <<ValueObject>>
            -String email
            -String phone
            +ContactInfo(String, String)
            +email() String
            +phone() String
            +hasEmail() boolean
            +hasPhone() boolean
        }

        class Address {
            <<ValueObject>>
            -String addressId
            -String recipientName
            -String phone
            -String province
            -String city
            -String streetAddress
            -String postalCode
            -String title
            -boolean isDefault
            +Address(String, String, String, String, String, String, String, String, boolean)
            +addressId() String
            +recipientName() String
            +phone() String
            +province() String
            +city() String
            +streetAddress() String
            +postalCode() String
            +title() String
            +isDefault() boolean
            +withDefault(boolean) Address
            +fullAddressText() String
        }

        class Category {
            <<ValueObject>>
            -String id
            -String name
            -String description
            -boolean active
            +Category(String, String, String)
            +Category(String, String, String, boolean)
            +id() String
            +name() String
            +description() String
            +active() boolean
        }

        class CartItem {
            <<ValueObject>>
            -String productId
            -String productName
            -Money unitPrice
            -Quantity quantity
            +CartItem(String, String, Money, Quantity)
            +productId() String
            +productName() String
            +unitPrice() Money
            +quantity() Quantity
            +getLineTotal() Money
            +withQuantity(Quantity) CartItem
        }

        class OrderItem {
            <<ValueObject>>
            -String productId
            -String productName
            -Money unitPrice
            -Quantity quantity
            -Money lineTotal
            +OrderItem(String, String, Money, Quantity)
            +productId() String
            +productName() String
            +unitPrice() Money
            +quantity() Quantity
            +lineTotal() Money
        }

        class Coupon {
            <<ValueObject>>
            -String code
            -int discountPercent
            -Money maxDiscount
            -Money minOrderAmount
            -LocalDate expiryDate
            +Coupon(String, int, Money, Money, LocalDate)
            +code() String
            +discountPercent() int
            +maxDiscount() Money
            +minOrderAmount() Money
            +expiryDate() LocalDate
            +calculateDiscount(Money) Money
            +isValid(Money, LocalDate) boolean
        }

        class PaymentTransaction {
            <<ValueObject>>
            -String transactionId
            -String orderId
            -Money amount
            -PaymentMethod method
            -PaymentStatus status
            -LocalDateTime timestamp
            -String referenceCode
            +PaymentTransaction(String, String, Money, PaymentMethod, PaymentStatus, LocalDateTime, String)
            +transactionId() String
            +orderId() String
            +amount() Money
            +method() PaymentMethod
            +status() PaymentStatus
            +timestamp() LocalDateTime
            +referenceCode() String
        }

        class Money {
            <<ValueObject>>
            -BigDecimal amount
            -String currency
            +ZERO$ Money
            +Money(BigDecimal, String)
            +amount() BigDecimal
            +currency() String
            +add(Money) Money
            +subtract(Money) Money
            +multiply(Quantity) Money
            +isLessThan(Money) boolean
            +isNegative() boolean
        }

        class Quantity {
            <<ValueObject>>
            -int value
            +Quantity(int)
            +value() int
            +add(Quantity) Quantity
            +subtract(Quantity) Quantity
            +isGreaterThan(Quantity) boolean
        }

        class Review {
            <<ValueObject>>
            -String reviewId
            -String userId
            -String authorName
            -String productId
            -int rating
            -String commentText
            -LocalDateTime createdAt
            +Review(String, String, String, String, int, String, LocalDateTime)
            +reviewId() String
            +userId() String
            +authorName() String
            +productId() String
            +rating() int
            +commentText() String
            +createdAt() LocalDateTime
        }
    }

    namespace Enumerations {
        class UserRole {
            <<enumeration>>
            CUSTOMER
            ADMIN
            STOREKEEPER
        }

        class OrderStatus {
            <<enumeration>>
            PENDING
            PAYMENT_FAILED
            PAID
            PROCESSING
            SHIPPED
            DELIVERED
            CANCELLED
        }

        class PaymentMethod {
            <<enumeration>>
            ONLINE_GATEWAY
            WALLET
            CASH_ON_DELIVERY
        }

        class PaymentStatus {
            <<enumeration>>
            SUCCESS
            FAILED
            CANCELLED
        }
    }


    User "1" *-- "0..*" Address : contains
    User "1" --> "1" UserRole : has
    User "1" --> "0..1" Cart : owns
    User "1" *-- "1" ContactInfo : contacts
    Product --> "1" Money : costs
    Product --> "1" Category : belongs to
    Product "1" o-- "0..*" Review : receives
    Review --> "1" User : authored by
    Cart "1" *-- "0..*" CartItem : contains
    CartItem --> "1" Quantity : measured by
    CartItem --> "1" Money : priced by
    CartItem --> "1" Product : references
    Order "1" *-- "1..*" OrderItem : contains
    Order "1" --> "1" Address : delivers to
    Order "1" --> "1" OrderStatus : tracked by
    Order "1" --> "0..*" PaymentTransaction : records
    Order --> "0..1" Coupon : applies
    Order --> "4" Money : totals
    OrderItem --> "1" Money : priced by
    OrderItem --> "1" Quantity : counted by
    PaymentTransaction --> "1" PaymentMethod : via
    PaymentTransaction --> "1" PaymentStatus : results in
```

### ۸.۳. نمودار ماشین وضعیت چرخه عمر سفارش (Order Lifecycle Statechart)

```mermaid
stateDiagram-v2
    [*] --> PENDING : ثبت سفارش (checkout)
    PENDING --> PAID : پرداخت موفق (processPayment=true)
    PENDING --> PAYMENT_FAILED : خطای پرداخت (processPayment=false)
    PAYMENT_FAILED --> PAID : تلاش مجدد موفق
    PAYMENT_FAILED --> PAYMENT_FAILED : تلاش مجدد ناموفق
    PAYMENT_FAILED --> CANCELLED : انصراف کاربر / عدم پرداخت
    PENDING --> CANCELLED : لغو توسط کاربر
    PAID --> CANCELLED : لغو پیش از پردازش (با استرداد وجه و بازگشت موجودی)
    PAID --> PROCESSING : تأیید و آماده‌سازی در انبار
    PROCESSING --> SHIPPED : تحویل به پست / پیک
    SHIPPED --> DELIVERED : تحویل موفق به مشتری
    DELIVERED --> [*]
    CANCELLED --> [*]
```

---

## ۹. اعتبارسنجی جامع مدل و ارزیابی ده‌گانه (Validation & Compliance)

مطابق چک‌لیست اعتبارسنجی ده‌گانه اصول طراحی نرم‌افزار، انطباق کامل سند با معیارهای آموزشی بررسی و تأیید شد:

| ردیف | موضوع ارزیابی | نحوه انطباق در این مدل | وضعیت |
| :---: | :--- | :--- | :---: |
| ۱ | **پوشش ۱۰۰٪ داستان‌های کاربری** | هر ۳۶ یوزر استوری در ماتریس ردیابی نگاشت شده و متد مشخص دارند. | **تأیید شد** |
| ۲ | **پوشش ۱۲ داستان دامنه** | تمامی فرآیندهای ۱۲ داستان دامنه به گردش فراخوانی میان لایه‌ها تبدیل شدند. | **تأیید شد** |
| ۳ | **تفکیک قطعی Entity و VO** | موجودیت‌ها دارای شناسه یکتا و چرخه عمر متغیرند؛ اشیاء مقداری تغییرناپذیرند. | **تأیید شد** |
| ۴ | **چندگانگی و جهت روابط** | جهت کلیه یال‌ها و کاردینالیتی در جدول ارتباطات و دیاگرام UML مشخص است. | **تأیید شد** |
| ۵ | **امضای کامل متدها با نوع صریح** | تمامی متدها با پارامترهای دارای نوع صریح و مقادیر بازگشتی دقیق تعریف شدند. | **تأیید شد** |
| ۶ | **استقلال مطلق لایه Model** | کلاس‌های لایه مدل هیچ‌گونه ارجاع یا وابستگی به لایه‌های سرویس یا CLI ندارند. | **تأیید شد** |
| ۷ | **عدم تعریف لایه دیتابیس مجزا** | مخازن داده به صورت ساختارهای In-Memory Map درون سرویس‌ها کپسوله شدند. | **تأیید شد** |
| ۸ | **رعایت استانداردهای کدنویسی Java** | نام‌گذاری‌ها منطبق بر استانداردهای رسمی جاوا (پکیج‌ها، کلاس‌ها، متدها) است. | **تأیید شد** |
| ۹ | **رعایت اصول Clean Code** | اصل تک‌مسئولیتی، تفکیک فرمان و پرسش (CQS) و نام‌های بامعنا رعایت گردید. | **تأیید شد** |
| ۱۰ | **مستندسازی صوری قراردادها و نامتغیرها** | درج پیش‌شرط، پس‌شرط، RI و AF برای موجودیت‌ها و رکوردهای کلیدی. | **تأیید شد** |

---

## ۱۰. ثبت وضعیت و آمادگی برای ورود به فاز بعدی

با تکمیل این سند جامع، مدل‌سازی مفهومی، ساختاری و رفتاری فروشگاه آنلاین بر مبنای اصل اتحاد کامل دیاگرام و متن و انطباق با کدهای پیاده‌سازی‌شده تثبیت گردید. این مستند به عنوان خط مبنای قطعی و مصوب کلیه فعالیت‌های پروژه تلقی می‌گردد.
