# مستندات فنی و مهندسی سامانه فروشگاه آنلاین
## Technical Documentation Center

این پوشه مرجع متمرکز و رسمی تمامی مستندات فنی، معماری و طراحی سامانه فروشگاه آنلاین شیءگرا (طراحی‌شده بر پایه Java 21 LTS در دانشکده مهندسی کامپیوتر دانشگاه صنعتی امیرکبیر) است.

---

### ساختار شاخه مستندات (`docs/`)

```text
docs/
├── README.md                                 # راهنمای جامع ساختار و دستورالعمل‌های مستندسازی
└── domain-model/                             # مشخصات فنی و مدل دامنه (فاز دوم پروژه)
    ├── domain-model-specification.md         # سند اصلی و خط مبنای ویرایشی به زبان Markdown (۱۰ فصل کامل)
    ├── domain-model-specification.tex        # سورس کد XeLaTeX با فونت وزیرمتن و بسته‌های فارسی‌ساز
    ├── domain-model-specification.pdf        # سند نهایی کامپایل‌شده (۲۵ صفحه با فهرست مطالب و جداول)
    ├── assets/
    │   ├── diagrams/                         # سورس دیاگرام‌های Mermaid (.mmd)
    │   │   ├── fig_1_entities_vs_vo.mmd     # دیاگرام تفکیک موجودیت‌ها و اشیاء مقداری
    │   │   ├── fig_2_services.mmd           # دیاگرام کلاس‌ها و روابط لایه سرویس
    │   │   ├── fig_3_cli_flow.mmd           # فلوچارت اجرای منوهای لایه واسط متنی (CLI)
    │   │   ├── fig_4_domain_model.mmd       # دیاگرام جامع کلاس‌های لایه مدل (Core Domain)
    │   │   └── fig_5_order_lifecycle.mmd    # دیاگرام ماشین وضعیت چرخه حیات سفارش
    │   ├── figures/                          # تصاویر نمودارها و لوگوی دانشگاه صنعتی امیرکبیر
    │   │   ├── aut_logo.png
    │   │   ├── fig_1.png / fig_1.svg
    │   │   ├── fig_2.png / fig_2.svg
    │   │   ├── fig_3.png / fig_3.svg
    │   │   ├── fig_4.png / fig_4.svg
    │   │   └── fig_5.png / fig_5.svg
    │   └── fonts/                            # فونت‌های استاندارد فارسی (خانواده Vazirmatn TTF)
    └── build/                                # ابزارها و اسکریپت‌های اتوماسیون ساخت و تولید PDF
        ├── header.tex                        # پیکربندی استایل‌ها، رنگ‌ها، tcolorbox و xepersian
        ├── generate_latex.py                 # اسکریپت پایتون جهت همگام‌سازی و تولید سورس TeX
        ├── render_figures.py                 # اسکریپت پایتون جهت رندر خودکار دیاگرام‌ها به SVG و PNG
        └── build_pdf.ps1                     # اسکریپت پاورشل ساخت آسان دو مرحله‌ای PDF با XeLaTeX
```

---

### منابع اولیه قابل ویرایش در برابر خروجی‌های کامپایل‌شده

* **سند ویرایشی مرجع (Primary Editable Document):**  
  فایل [`docs/domain-model/domain-model-specification.md`](domain-model/domain-model-specification.md) سند رسمی و مرجع اصلی ویرایش مدل دامنه است. این سند حاوی تمام ۱۰ فصل استاندارد، قراردادهای صوری (`REQUIRES / MODIFIES / EFFECTS`)، نامتغیرهای بازنمایی (`RI`)، توابع انتزاع (`AF`)، جداول ردیابی داستان‌های کاربری و قوانین ۱۴‌گانه کسب‌وکار است.
* **سورس‌های دیاگرام (Diagram Sources):**  
  سورس‌های موجود در پوشه `docs/domain-model/assets/diagrams/` فایل‌های متنی استاندارد Mermaid هستند که مستقیماً در Markdown و ابزارهای رندر دیاگرام پشتیبانی می‌شوند.
* **سورس و خروجی تایپ‌ستینگ (Typesetting Source & Publication Artifact):**  
  فایل `domain-model-specification.tex` و فایل `domain-model-specification.pdf` نسخه‌های آکادمیک و حروف‌چینی‌شده برای تحویل و مطالعه رسمی هستند.

---

### دستورالعمل ساخت مجدد PDF (Rebuilding the PDF)

برای کامپایل مجدد نسخه PDF از طریق ابزار `XeLaTeX` (موجود در توزیع‌های TeX Live / TinyTeX / MiKTeX):

#### روش ۱: اجرای تک‌دستوری از طریق PowerShell
```powershell
cd docs/domain-model/build
.\build_pdf.ps1
```

#### روش ۲: کامپایل دستی دو مرحله‌ای با دستور مستقیم XeLaTeX
```powershell
cd docs/domain-model
xelatex -interaction=nonstopmode domain-model-specification.tex
xelatex -interaction=nonstopmode domain-model-specification.tex
```
*(اجرای دو مرحله‌ای جهت استخراج درست فهرست مطالب و شماره‌گذاری ارجاعات متقابل جداول و شکل‌ها ضروری است).*
