# -*- coding: utf-8 -*-
"""Generate synchronized domain-model-specification.tex for Online Shop"""
import os
import sys

def build_tex():
    build_dir = os.path.dirname(os.path.abspath(__file__))
    doc_dir = os.path.dirname(build_dir)
    tex_path = os.path.join(doc_dir, "domain-model-specification.tex")

    content = r"""\documentclass[11pt,a4paper]{article}

% --- Load Modular Header & Styles ---
\input{build/header.tex}

\begin{document}

% =========================================================================
% COVER PAGE
% =========================================================================
\begin{titlepage}
\centering
\vspace*{0.8cm}
\includegraphics[height=2.4cm]{assets/figures/aut_logo.png}\\[0.8cm]
{\Large\bfseries\color{autnavy}دانشگاه صنعتی امیرکبیر (پلی‌تکنیک تهران)}\\[0.3cm]
{\large\color{autblue}دانشکده مهندسی کامپیوتر}\\[0.2cm]
{\normalsize\color{gray}درس اصول طراحی نرم‌افزار --- نیم‌سال اول ۱۴۰۵--۱۴۰۶}\\[1.5cm]

\begin{tcolorbox}[colback=cardbg,colframe=autnavy,arc=6pt,boxrule=1.5pt,center,width=0.95\textwidth]
\centering
\vspace{0.4cm}
{\huge\bfseries\color{autnavy}سند جامع مشخصات مدل دامنه}\\[0.3cm]
{\Large\bfseries\color{autblue}فروشگاه آنلاین شیءگرا}\\[0.2cm]
{\large\color{autteal}\lr{Object-Oriented Domain Model Specification}}\\[0.2cm]
{\small\color{gray}نسخه نهایی یکپارچه‌شده بر مبنای اصل اتحاد دیاگرام و پیاده‌سازی \lr{(Reconciled Baseline)}}\\[0.3cm]
\end{tcolorbox}

\vspace{1.8cm}
\begin{tcolorbox}[colback=white,colframe=bordercol,arc=4pt,boxrule=0.8pt,width=0.82\textwidth]
\begin{tabular}{ll}
\textbf{موضوع فاز دوم:} & مدل‌سازی دقیق دامنه، روابط کلاس‌ها، قوانین کسب‌وکار و قراردادها \\
\textbf{فناوری هدف:} & جاوا ۲۱ (\lr{Java 21 LTS}) با رکوردهای تغییرناپذیر \\
\textbf{معماری حاکم:} & معماری چندلایه‌ای ناب دامنه-محور (\lr{Domain-Driven Layered}) \\
\textbf{وضعیت مدل:} & مصوب و منطبق بر اصل اتحاد متدها و مشخصات پیاده‌سازی \\
\textbf{تاریخ نگارش:} & آبان ۱۴۰۵ / نوامبر ۲۰۲۶ \\
\end{tabular}
\end{tcolorbox}

\vfill
{\small\color{gray}تهیه‌شده بر مبنای محتوای آموزشی اسلایدها و جزوات درسی استاد مهدی مزینانی}
\end{titlepage}

% =========================================================================
% TABLE OF CONTENTS
% =========================================================================
\newpage
\tableofcontents
\newpage

% =========================================================================
% SECTION 1
% =========================================================================
\section{شناسنامه و رویکرد معماری مدل \lr{(Architectural Overview)}}

این مستند، به عنوان منبع موثق و خط مبنای واحد (\lr{Single Source of Truth})، مشخصات کامل معماری، قوانین کسب‌وکار و مدل دامنه سامانه فروشگاه آنلاین را تبیین می‌نماید. مدل حاضر بر اساس نیازمندی‌های فاز دوم پروژه و با وفاداری به آموزه‌های چهار جزوه درسی تدوین شده است:
\begin{itemize}[leftmargin=1.5cm]
    \item \lr{\texttt{clean-code.pdf}}: رعایت اسامی بامعنا، متدهای تک‌منظوره و کوچک، تفکیک پرسش از فرمان (\lr{CQS}) و کاهش وابستگی‌ها.
    \item \lr{\texttt{dataAbstraction.pdf}}: تفکیک دقیق انواع جهش‌پذیر و تغییرناپذیر، نامتغیرهای بازنمایی (\lr{RI}) و توابع انتزاع (\lr{AF}) و پیشگیری از افشای بازنمایی.
    \item \lr{\texttt{java-code-conventions.pdf}}: رعایت استانداردها و قراردادهای رسمی کدنویسی جاوا.
    \item \lr{\texttt{procedural-abstraction.pdf}}: تدوین دقیق پیش‌شرط‌ها (\lr{REQUIRES})، اثرات جانبی (\lr{MODIFIES}) و نتایج تضمین‌شده (\lr{EFFECTS}).
\end{itemize}

\subsection{اصول بنیادین حاکم بر مدل‌سازی}
\begin{enumerate}
    \item \textbf{معماری چندلایه‌ای ناب (\lr{Layered Architecture}):} تفکیک قطعی لایه‌های واسط کاربری متنی (\lr{CLI})، لایه سرویس‌های کاربردی (\lr{Service}) و لایه مدل کسب‌وکار (\lr{Model}). لایه مدل کاملاً مستقل بوده و هیچ وابستگی به لایه‌های بالاتر ندارد.
    \item \textbf{استفاده از قابلیت‌های نوین جاوا ۲۱:} مدل‌سازی کلیه اشیاء مقداری تغییرناپذیر به صورت رکوردهای جاوا (\lr{\texttt{record}}) با سازنده‌های فشرده (\lr{Compact Constructors}).
    \item \textbf{مشخصه‌سازی علمی عملیات:} تدوین قرارداد سه‌بخشی \lr{\texttt{REQUIRES/MODIFIES/EFFECTS}} برای تمامی متدهای حساس لایه‌های مدل و سرویس.
    \item \textbf{اصل اتحاد و یکپارچگی مدل (\lr{Union Baseline}):} تمامی عناصر ساختاری و رفتاری موجود در کاتالوگ، دیاگرام‌های \lr{UML} و کدهای پیاده‌سازی‌شده با یکدیگر هماهنگ شده‌اند.
\end{enumerate}

\subsection{معماری لایه‌ای و تفکیک مسئولیت‌ها}
سامانه به چهار بخش اصلی تفکیک شده است که جریان وابستگی صرفاً از لایه‌های بالاتر به لایه‌های پایین‌تر مجاز است:

\begin{table}[H]
\centering
\small
\begin{tabularx}{\textwidth}{|>{\bfseries\arraybackslash}p{2.5cm}|>{\raggedright\arraybackslash}p{3cm}|>{\raggedright\arraybackslash}X|>{\raggedright\arraybackslash}p{4cm}|}
\hline
\rowcolor{cardbg}
بخش / لایه & پکیج مربوطه & مسئولیت اصلی & وابستگی‌های مجاز \\
\hline
\lr{Model} & \lr{\texttt{org.example.model}} & داده‌ها، اشیاء مقداری، موجودیت‌ها، نامتغیرها و رفتارهای داخلی اشیاء. & کتابخانه استاندارد جاوا \\
\hline
\lr{Service} & \lr{\texttt{org.example.service}} & اجرای سناریوهای کاربردی، احراز هویت، کاتالوگ، سبد، پرداخت، سفارش، نظرات و مخازن داده درون‌حافظه‌ای. & \lr{Model}، استثناها و ابزارهای عمومی \\
\hline
\lr{CLI} & \lr{\texttt{org.example.cli}} & دریافت ورودی‌ها، اعتبارسنجی مرزی، فراخوانی سرویس‌ها و نمایش منوها. & \lr{Service} و \lr{Model} \\
\hline
\lr{Main \& Util} & \lr{\texttt{org.example}} & ساخت مخازن داده مشترک، اتصال اجزا (\lr{DI})، ابزار هشینگ و راه‌اندازی. & تمامی اجزای لازم برای اجرا \\
\hline
\end{tabularx}
\caption{تفکیک مسئولیت‌ها و وابستگی‌های مجاز در معماری سامانه}
\label{tab:architecture_layers}
\end{table}

\subsection{قوانین و رفتارهای حاکم بر دامنه \lr{(Core Domain Rules \& Policies)}}
\begin{enumerate}
    \item \textbf{هویت و یکتایی در ثبت‌نام:} ثبت‌نام حداقل به ایمیل یا موبایل نیاز دارد. هر مشخصه واردشده باید معتبر و غیرتکراری باشد. شیء مقداری \lr{\texttt{ContactInfo}} فرمت \lr{RFC} ایمیل و شماره همراه ایران (\lr{\texttt{09[0-9]\{9\}}}) را اعتبارسنجی می‌کند؛ سرویس‌های \lr{\texttt{AuthService}} و \lr{\texttt{UserService}} عدم تکراری بودن را تضمین می‌کنند.
    \item \textbf{ورود و ابطال نشست:} ورود با ایمیل یا موبایل انجام می‌گیرد. رمز عبور به صورت هش امن نگهداری شده و هرگز در پروفایل فاش نمی‌شود. خروج کاربر، توکن نشست جاری را باطل می‌کند. بازیابی رمز، کلیه نشست‌های قبلی کاربر را منقضی می‌سازد.
    \item \textbf{شبیه‌سازی بازیابی رمز عبور:} در محیط متنی، کد ۶ رقمی موقت کنسولی با اعتبار ۱۰ دقیقه تولید می‌شود و پس از یک‌بار مصرف ابطال می‌گردد.
    \item \textbf{رؤیت‌پذیری کاتالوگ:} تنها محصول فعال در دسته فعال به خریدار نمایش داده می‌شود. اگر محصولی پس از افزودن به سبد غیرفعال شود، در مرحله تسویه‌حساب یا پرداخت متوقف می‌شود.
    \item \textbf{مثبت بودن تعداد و عدم تجاوز از موجودی:} تعداد اقلام همواره اکیداً مثبت (\lr{$Quantity > 0$}) بوده و از موجودی انبار بیشتر نمی‌شود. افزودن مجدد کالا، تعداد همان سطر سبد را افزایش می‌دهد.
    \item \textbf{محدودیت‌های متنی و ارقامی:} طول جستجوی کالا بین ۳ تا ۱۰۰ کاراکتر، طول نظر کالا حداکثر ۲۰۰۰ کاراکتر، امتیاز ۱ تا ۵ ستاره، کد پستی دقیقاً ۱۰ رقمی و تلفن همراه منطبق بر الگوی معتبر است.
    \item \textbf{محاسبات مالی و کوپن:} قیمت نهایی سفارش برابر است با مجموع قیمت اقلام منهای تخفیف کوپن به علاوه هزینه ارسال (فعلاً صفر). کوپن باید منقضی نشده باشد و شرط حداقل خرید را احراز کند.
    \item \textbf{پرداخت و کسر اتمیک موجودی:} پرداخت فقط برای سفارش‌های در وضعیت \lr{\texttt{PENDING}} یا \lr{\texttt{PAYMENT\_FAILED}} مجاز است. پیش از کسر موجودی، کفایت موجودی کل اقلام به صورت اتمیک بررسی می‌شود. شکست پرداخت موجودی را تغییر نمی‌دهد و امکان تلاش مجدد محفوظ است.
    \item \textbf{تسویه جزئی سبد خرید:} هنگام پرداخت موفق، صرفاً اقلامی از سبد خرید حذف می‌شوند که در سفارش قطعی شده‌اند و از زمان تسویه تا پرداخت بدون تغییر مانده‌اند. اقلام جدید یا ویرایش‌شده در سبد حفظ می‌شوند.
    \item \textbf{استرداد موجودی در لغو سفارش:} لغو سفارش پرداخت‌شده منجر به بازگشت قطعی موجودی کالاها به انبار می‌گردد. لغو مجدد سفارش لغوشده ممنوع است.
    \item \textbf{انزوای داده‌های مشتری:} مشتری فقط به جزئیات، پرداخت و لغو سفارش‌های متعلق به شناسه کاربری خود دسترسی دارد (متدهای دارای \lr{\texttt{userId}}).
    \item \textbf{کنترل دسترسی نقش‌محور (\lr{RBAC}):} صرفاً نقش \lr{\texttt{ADMIN}} مجاز به ورود به پنل مدیریت است. نقش \lr{\texttt{STOREKEEPER}} در مدل شمارشی جهت سازگاری تعریف شده اما دسترسی مدیریتی ندارد.
    \item \textbf{تصاویر کالا:} تصاویر به صورت \lr{URL}های معتبر اینترنتی ذخیره شده و در کنسول نمایش داده می‌شوند و توسط مدیر قابل ویرایش هستند.
    \item \textbf{امتیاز محبوبیت و جدیدترین:} مرتب‌سازی جدیدترین بر مبنای تاریخ ثبت (\lr{\texttt{createdAt}}) و محبوب‌ترین بر اساس شمارنده فروش تجمعی (\lr{\texttt{soldCount}}) در پرداخت‌های موفق انجام می‌گیرد.
\end{enumerate}

% =========================================================================
% SECTION 2
% =========================================================================\newpage
\section{ماتریس ردیابی جامع ۳۶ داستان کاربری و ۱۲ داستان دامنه \lr{(Traceability Matrix)}}

برای اثبات پوشش ۱۰۰٪ نیازمندی‌ها، ماتریس جدول~\ref{tab:traceability} نحوه اتصال داستان‌های کاربری (\lr{US}) و داستان‌های دامنه (\lr{DS}) را به کلاس‌ها و متدهای مدل مشخص می‌سازد:

\begin{table}[H]
\centering
\small
\begin{tabularx}{\textwidth}{|>{\raggedleft\arraybackslash}p{2.2cm}|>{\centering\arraybackslash}p{1.9cm}|>{\centering\arraybackslash}p{1.2cm}|>{\raggedright\arraybackslash}X|>{\raggedright\arraybackslash}X|}
\hline
\rowcolor{cardbg}
\textbf{حوزه کارکردی} & \textbf{داستان کاربری} & \textbf{داستان دامنه} & \textbf{کلاس‌های لایه \lr{Model}} & \textbf{متدهای هماهنگ‌کننده لایه \lr{Service}} \\
\hline
\textbf{ثبت‌نام و ورود} & \lr{US 01, 02, 03} & \lr{DS 01, 02} & \lr{User, UserRole, ContactInfo} & \lr{AuthService.register, login, logout} \\
\hline
\textbf{بازیابی رمز} & \lr{US 04} & \lr{DS 03} & \lr{User, PasswordReset} & \lr{AuthService.requestPasswordReset, resetPassword} \\
\hline
\textbf{پروفایل و آدرس} & \lr{US 05, 06, 07} & \lr{DS 04} & \lr{User, Address, ContactInfo} & \lr{UserService.getProfile, updateProfile, addAddress, updateAddress, removeAddress, setDefaultAddress} \\
\hline
\textbf{کاتالوگ کالا} & \lr{US 08--14} & \lr{DS 05} & \lr{Product, Category, Money, Quantity} & \lr{ProductService.getAllActiveProducts, getProductById, searchProducts, filterAndSort} \\
\hline
\textbf{سبد خرید} & \lr{US 15--19} & \lr{DS 06} & \lr{Cart, CartItem, Quantity, Product} & \lr{CartService.getCart, addItem, updateItemQuantity, removeItem, removeCheckedOutItems, clearCart} \\
\hline
\textbf{تسویه‌حساب} & \lr{US 20--23} & \lr{DS 07} & \lr{Order, OrderItem, Coupon, Address} & \lr{OrderService.checkout} \\
\hline
\textbf{پرداخت و تراکنش} & \lr{US 24, 25} & \lr{DS 07, 08} & \lr{Order, PaymentTransaction, PaymentMethod} & \lr{OrderService.processPayment} \\
\hline
\textbf{تاریخچه سفارش} & \lr{US 26, 27, 28} & \lr{DS 09} & \lr{Order, OrderItem, OrderStatus} & \lr{OrderService.getOrderHistory, getOrderDetails, cancelOrder} \\
\hline
\textbf{نظرات و امتیاز} & \lr{US 30, 31, 32} & \lr{DS 10} & \lr{Review, Product} & \lr{ReviewService.addReview, getProductReviews} \\
\hline
\textbf{مدیریت کاتالوگ} & \lr{US 33, 34, 35} & \lr{DS 11} & \lr{Product, Category, Money} & \lr{ProductService.addProduct, updateProduct, setProductImages, deactivateProduct} \\
\hline
\textbf{انبارداری و پردازش} & \lr{US 29, 36} & \lr{DS 12} & \lr{Product, Order, OrderStatus, Quantity} & \lr{ProductService.adjustStock; OrderService.getAllOrders, getOrdersByStatus, updateOrderStatus} \\
\hline
\end{tabularx}
\caption{ماتریس ردیابی جامع داستان‌های کاربری و دامنه به اجزای مدل و سرویس}
\label{tab:traceability}
\end{table}

% =========================================================================
% SECTION 3
% =========================================================================
\section{تفکیک مفاهیم دامنه: موجودیت‌ها در برابر اشیاء مقداری \lr{(Entities vs. Value Objects)}}

بر اساس آموزه‌های جزوه \lr{\texttt{dataAbstraction.pdf}}، تفکیک مفاهیم بر پایه اصول شیءگرایی و مهندسی نرم‌افزار به شرح زیر توجیه می‌گردد:

\begin{figure}[H]
\centering
\includegraphics[width=\textwidth]{assets/figures/fig_1.png}
\caption{تفکیک مفهومی موجودیت‌ها (\lr{Entities}) و اشیاء مقداری (\lr{Value Objects})}
\label{fig:concept_hierarchy}
\end{figure}

\subsection{موجودیت‌ها \lr{(Entities)}}
\begin{enumerate}
    \item \textbf{\lr{User}:} دارای شناسه یکتا (\lr{\texttt{id}}). وضعیت حساب، نشانی‌ها و مشخصات آن در طول زمان تغییر می‌کند.
    \item \textbf{\lr{Product}:} دارای شناسه یکتا (\lr{\texttt{id}}). موجودی انبار، قیمت، آمار فروش و وضعیت فعال بودن آن جهش‌پذیر است.
    \item \textbf{\lr{Cart}:} سبد خرید مشتری که اقلام آن در طول نشست کاری افزوده، ویرایش یا حذف می‌شوند (\lr{Mutable Aggregate}).
    \item \textbf{\lr{Order}:} سند رسمی خرید با شناسه یکتا (\lr{\texttt{orderId}}) که وضعیت چرخه عمر آن دگرگون می‌شود.
\end{enumerate}

\subsection{اشیاء مقداری \lr{(Value Objects)}}
\begin{enumerate}
    \item \textbf{\lr{Quantity}:} تعداد گسسته اقلام با تضمین نامتغیر $value > 0$.
    \item \textbf{\lr{Money}:} مدل‌سازی مالی دقیق با \lr{\texttt{BigDecimal}} و واحد پولی جهت پیشگیری از خطای محاسبات اعشاری.
    \item \textbf{\lr{Address}:} نشانی پستی تحویل سفارش به صورت تغییرناپذیر با کد پستی ده‌رقمی.
    \item \textbf{\lr{ContactInfo}:} شیء مقداری تغییرناپذیر اطلاعات تماس شامل ایمیل و تلفن همراه معتبر.
    \item \textbf{\lr{CartItem}:} سطر سبد خرید شامل کالا، قیمت و تعداد.
    \item \textbf{\lr{OrderItem}:} تصویر پایدار و تاریخی اقلام در لحظه ثبت سفارش (\lr{Immutable Snapshot}).
    \item \textbf{\lr{Review}:} دیدگاه و امتیاز ثبت‌شده برای کالا با سقف ۲۰۰۰ کاراکتر متن.
    \item \textbf{\lr{Coupon}:} ضوابط و تخفیف کوپن.
    \item \textbf{\lr{Category}:} دسته‌بندی کالا در کاتالوگ فروشگاه به همراه پرچم فعال بودن.
    \item \textbf{\lr{PaymentTransaction}:} سند ثبت تراکنش مالی پرداخت سفارش.
    \item \textbf{انواع شمارشی (\lr{Enums}):} \lr{UserRole}, \lr{OrderStatus}, \lr{PaymentMethod}, \lr{PaymentStatus}.
\end{enumerate}

% =========================================================================
% SECTION 4
% =========================================================================
\newpage
\section{کاتالوگ جامع کلاس‌های لایه مدل \lr{(Domain Model Catalog)}}

در این بخش، کلیه اجزای کلاس‌های لایه \lr{Model} به طور کامل و منطبق بر اصل اتحاد مستند می‌شوند.

% --- 4.1 Quantity ---
\subsection{کلاس \lr{Quantity} \lr{(Value Object --- Record)}}
\textbf{هدف:} مدل‌سازی ریاضی و کسب‌وکاری تعداد اقلام در سبد، سفارش و انبار.\\
\textbf{فیلدها:}
\begin{itemize}
    \item \lr{\texttt{private final int value}}: مقدار عددی صحیح مثبت.
\end{itemize}
\textbf{سازنده:}
\begin{itemize}
    \item \lr{\texttt{public Quantity(int value)}}: سازنده فشرده رکورد با اعتبارسنجی نامتغیر.
\end{itemize}
\textbf{متدها:}
\begin{itemize}
    \item \lr{\texttt{public int value()}}: دریافت مقدار عددی (\lr{Observer} --- تولید خودکار توسط کامپایلر رکورد).
    \item \lr{\texttt{public Quantity add(Quantity other)}}: تولید تعداد جدید حاصل جمع (\lr{Producer}).
    \item \lr{\texttt{public Quantity subtract(Quantity other)}}: تولید تعداد جدید حاصل تفریق (\lr{Producer}).
    \item \lr{\texttt{public boolean isGreaterThan(Quantity other)}}: مقایسه بزرگی (\lr{Observer}).
\end{itemize}
\textbf{نامتغیر نمایش (\lr{RI}):} \lr{\texttt{value > 0}}.\\
\textbf{تابع انتزاع (\lr{AF}):} \lr{\texttt{AF(q) = an exact strictly positive integer count equal to q.value}}.
\begin{contractbox}
\begin{latin}
\setlength{\parindent}{0pt}
\setlength{\parskip}{2pt}
\small
\textbf{\color{autnavy}\texttt{Constructor:}} \\
\hspace*{1.5em}\textbf{\color{darkred}\texttt{REQUIRES:}} \texttt{value > 0} \\
\hspace*{1.5em}\textbf{\color{autblue}\texttt{MODIFIES:}} \texttt{this} \\
\hspace*{1.5em}\textbf{\color{autteal}\texttt{EFFECTS:}}  \texttt{Initializes Quantity; throws IllegalArgumentException if value <= 0.}

\vspace{4pt}\hrule\vspace{4pt}

\textbf{\color{autnavy}\texttt{add(Quantity other):}} \\
\hspace*{1.5em}\textbf{\color{darkred}\texttt{REQUIRES:}} \texttt{other != null} \\
\hspace*{1.5em}\textbf{\color{autblue}\texttt{MODIFIES:}} \texttt{nothing} \\
\hspace*{1.5em}\textbf{\color{autteal}\texttt{EFFECTS:}}  \texttt{Returns new Quantity(this.value + other.value); throws NullPointerException if null.}

\vspace{4pt}\hrule\vspace{4pt}

\textbf{\color{autnavy}\texttt{subtract(Quantity other):}} \\
\hspace*{1.5em}\textbf{\color{darkred}\texttt{REQUIRES:}} \texttt{other != null and this.value > other.value} \\
\hspace*{1.5em}\textbf{\color{autblue}\texttt{MODIFIES:}} \texttt{nothing} \\
\hspace*{1.5em}\textbf{\color{autteal}\texttt{EFFECTS:}}  \texttt{Returns new Quantity(this.value - other.value); throws IllegalArgumentException if other.value >= this.value.}
\end{latin}
\end{contractbox}

% --- 4.2 Money ---
\subsection{کلاس \lr{Money} \lr{(Value Object --- Record)}}
\textbf{هدف:} مدل‌سازی مالی دقیق جهت قیمت‌ها، مالیات، هزینه ارسال و تخفیف‌ها.\\
\textbf{فیلدها:}
\begin{itemize}
    \item \lr{\texttt{private final BigDecimal amount}}: مبلغ عددی با مقیاس استاندارد ۲ رقم اعشار.
    \item \lr{\texttt{private final String currency}}: واحد پولی استاندارد (مانند \lr{\texttt{"IRR"}} یا \lr{\texttt{"TOMAN"}}).
\end{itemize}
\textbf{ثابت‌ها:}
\begin{itemize}
    \item \lr{\texttt{public static final Money ZERO = new Money(BigDecimal.ZERO, "IRR");}}
\end{itemize}
\textbf{سازنده:}
\begin{itemize}
    \item \lr{\texttt{public Money(BigDecimal amount, String currency)}}: سازنده فشرده رکورد.
\end{itemize}
\textbf{متدها:}
\begin{itemize}
    \item \lr{\texttt{public BigDecimal amount()}}: دریافت مبلغ (\lr{Observer}).
    \item \lr{\texttt{public String currency()}}: دریافت واحد پول (\lr{Observer}).
    \item \lr{\texttt{public Money add(Money other)}}: جمع دو مبلغ هم‌واحد (\lr{Producer}).
    \item \lr{\texttt{public Money subtract(Money other)}}: تفریق دو مبلغ هم‌واحد (\lr{Producer}).
    \item \lr{\texttt{public Money multiply(Quantity quantity)}}: ضرب مبلغ در تعداد (\lr{Producer}).
    \item \lr{\texttt{public boolean isLessThan(Money other)}}: مقایسه کمتر بودن مبالغ (\lr{Observer}).
    \item \lr{\texttt{public boolean isNegative()}}: بررسی منفی بودن مبلغ (\lr{Observer}).
\end{itemize}
\textbf{نامتغیر نمایش (\lr{RI}):} \lr{\texttt{amount != null \&\& currency != null \&\& !currency.isBlank() \&\& amount.scale() == 2 \&\& amount.signum() >= 0}}.\\
\textbf{تابع انتزاع (\lr{AF}):} \lr{\texttt{AF(m) = a monetary value of m.amount in currency m.currency}}.

% --- 4.3 Address ---
\subsection{کلاس \lr{Address} \lr{(Value Object --- Record)}}
\textbf{هدف:} نگهداری مشخصات نشانی تحویل کالا مطابق با \lr{US 07} و قوانین پستی کشور.\\
\textbf{فیلدها و مؤلفه‌های رکورد:}
\begin{itemize}
    \item \lr{\texttt{private final String addressId}}: شناسه محلی نشانی (مانند \lr{\texttt{"ADDR-1"}}).
    \item \lr{\texttt{private final String recipientName}}: نام گیرنده تحویل‌گیرنده.
    \item \lr{\texttt{private final String phone}}: شماره تماس همراه گیرنده.
    \item \lr{\texttt{private final String province}}: استان مقصد.
    \item \lr{\texttt{private final String city}}: شهر مقصد.
    \item \lr{\texttt{private final String streetAddress}}: نشانی پستی دقیق (خیابان، پلاک، واحد).
    \item \lr{\texttt{private final String postalCode}}: کد پستی ده‌رقمی معتبر مطابق با الگوی \lr{\texttt{\^{}[0-9]\{10\}\$}}.
    \item \lr{\texttt{private final String title}}: عنوان نشانی (نظیر منزل، شرکت).
    \item \lr{\texttt{private final boolean isDefault}}: پرچم پیش‌فرض بودن نشانی.
\end{itemize}
\textbf{متدها:}
\begin{itemize}
    \item اکسسورهای رکورد: \lr{\texttt{addressId()}}, \lr{\texttt{recipientName()}}, \lr{\texttt{phone()}}, \lr{\texttt{province()}}, \lr{\texttt{city()}}, \lr{\texttt{streetAddress()}}, \lr{\texttt{postalCode()}}, \lr{\texttt{title()}}, \lr{\texttt{isDefault()}}.
    \item \lr{\texttt{public Address withDefault(boolean isDefault)}}: نسخه جدید با پرچم به‌روزرسانی‌شده (\lr{Producer}).
    \item \lr{\texttt{public String fullAddressText()}}: قالب‌بندی متنی کامل آدرس جهت درج روی فاکتور (\lr{Observer}).
\end{itemize}

% --- 4.4 User ---
\subsection{کلاس \lr{User} \lr{(Entity)}}
\textbf{هدف:} هویت و حساب کاربری خریداران و مدیران سامانه (\lr{US 01} تا \lr{US 07}).\\
\textbf{فیلدها:}
\begin{itemize}
    \item \lr{\texttt{private final String id}}: شناسه یکتا و تغییرناپذیر کاربر.
    \item \lr{\texttt{private String fullName}}: نام و نام خانوادگی.
    \item \lr{\texttt{private String email}}: آدرس ایمیل منحصر‌به‌فرد.
    \item \lr{\texttt{private String phone}}: شماره تلفن همراه معتبر.
    \item \lr{\texttt{private ContactInfo contactInfo}}: شیء مقداری کانال‌های ارتباطی کاربر.
    \item \lr{\texttt{private String passwordHash}}: هش امنیتی رمز عبور (\lr{SHA-256}).
    \item \lr{\texttt{private UserRole role}}: نقش کاربری (\lr{CUSTOMER, ADMIN, STOREKEEPER}).
    \item \lr{\texttt{private boolean active}}: وضعیت فعال یا غیرفعال بودن حساب.
    \item \lr{\texttt{private final LocalDateTime createdAt}}: زمان ایجاد حساب.
    \item \lr{\texttt{private final List<Address> addresses}}: فهرست نشانی‌های ثبت‌شده.
\end{itemize}
\textbf{متدها:}
\begin{itemize}
    \item \lr{\texttt{public String getId()}}, \lr{\texttt{public String getFullName()}}, \lr{\texttt{public String getEmail()}}, \lr{\texttt{public String getPhone()}}, \lr{\texttt{public ContactInfo getContactInfo()}}, \lr{\texttt{public UserRole getRole()}}, \lr{\texttt{public boolean isActive()}}, \lr{\texttt{public LocalDateTime getCreatedAt()}} (\lr{Observers}).
    \item \lr{\texttt{public void activate()}}, \lr{\texttt{public void deactivate()}} (\lr{Mutators}).
    \item \lr{\texttt{public void updateProfile(String newFullName, String newPhone)}} (\lr{Mutator}).
    \item \lr{\texttt{public void changePassword(String newPasswordHash)}} (\lr{Mutator}).
    \item \lr{\texttt{public void addAddress(Address address)}}, \lr{\texttt{public void updateAddress(Address updatedAddress)}}, \lr{\texttt{public void removeAddress(String addressId)}}, \lr{\texttt{public void setDefaultAddress(String addressId)}} (\lr{Mutators}).
    \item \lr{\texttt{public Optional<Address> getDefaultAddress()}} (\lr{Observer}).
    \item \lr{\texttt{public List<Address> getAddresses()}}: دریافت کپی غیرقابل تغییر نشانی‌ها (\lr{Observer} --- محافظت با \lr{\texttt{List.copyOf}}).
\end{itemize}

% --- 4.5 Product ---
\subsection{کلاس \lr{Product} \lr{(Entity)}}
\textbf{هدف:} مدل‌سازی مشخصات کالا در کاتالوگ فروشگاه آنلاین (\lr{US 08--14, 33--36}).\\
\textbf{فیلدها:}
\begin{itemize}
    \item \lr{\texttt{private final String id}}: شناسه یکتای محصول.
    \item \lr{\texttt{private String name}}: عنوان تجاری کالا.
    \item \lr{\texttt{private String description}}: شرح تفصیلی کالا.
    \item \lr{\texttt{private Money price}}: قیمت فعلی کالا.
    \item \lr{\texttt{private int availableStock}}: تعداد موجودی فیزیکی در انبار ($availableStock \ge 0$).
    \item \lr{\texttt{private String categoryId}}: شناسه دسته‌بندی والد.
    \item \lr{\texttt{private boolean active}}: پرچم فعال بودن کالا در ویترین.
    \item \lr{\texttt{private double averageRating}}: میانگین امتیاز نظرات (۰٫۰ تا ۵٫۰).
    \item \lr{\texttt{private int reviewCount}}: تعداد کل دیدگاه‌های ثبت‌شده.
    \item \lr{\texttt{private final LocalDateTime createdAt}}: زمان ایجاد و ورود کالا به سامانه.
    \item \lr{\texttt{private int soldCount}}: شمارنده تجمیعی تعداد اقلام فروخته‌شده در پرداخت‌های موفق.
    \item \lr{\texttt{private List<String> imageUrls}}: فهرست آدرس‌های اینترنتی تصاویر کالا.
\end{itemize}
\textbf{متدها:}
\begin{itemize}
    \item اکسسورها: \lr{\texttt{getId()}}, \lr{\texttt{getName()}}, \lr{\texttt{getDescription()}}, \lr{\texttt{getPrice()}}, \lr{\texttt{getAvailableStock()}}, \lr{\texttt{getCategoryId()}}, \lr{\texttt{isActive()}}, \lr{\texttt{getAverageRating()}}, \lr{\texttt{getReviewCount()}}, \lr{\texttt{getCreatedAt()}}, \lr{\texttt{getSoldCount()}}, \lr{\texttt{getImageUrls()}}.
    \item \lr{\texttt{public boolean isAvailable(Quantity required)}}: بررسی کفایت موجودی (\lr{Observer}).
    \item \lr{\texttt{public void updateDetails(String name, String desc, Money price, String categoryId)}} (\lr{Mutator}).
    \item \lr{\texttt{public void setActive(boolean active)}}: تغییر وضعیت انتشار توسط مدیر (\lr{Mutator}).
    \item \lr{\texttt{public void deductStock(Quantity quantity)}}: کسر قطعی موجودی کالا (\lr{Mutator}).
    \item \lr{\texttt{public void restock(Quantity quantity)}}: شارژ موجودی انبار (\lr{Mutator}).
    \item \lr{\texttt{public void recordSale(Quantity quantity)}}: ثبت فروش و افزایش \lr{\texttt{soldCount}} (\lr{Mutator}).
    \item \lr{\texttt{public void setImageUrls(List<String> imageUrls)}}: به‌روزرسانی لینک تصاویر (\lr{Mutator}).
    \item \lr{\texttt{public void updateRatingStats(int newRatingScore)}}: محاسبه مجدد میانگین امتیاز (\lr{Mutator}).
\end{itemize}

% --- 4.6 CartItem ---
\subsection{کلاس \lr{CartItem} \lr{(Value Object --- Record)}}
\textbf{هدف:} مدل‌سازی هر سطر در سبد خرید مشتری (\lr{US 15--19}).\\
\textbf{فیلدها و مؤلفه‌ها:} \lr{\texttt{productId: String}}, \lr{\texttt{productName: String}}, \lr{\texttt{unitPrice: Money}}, \lr{\texttt{quantity: Quantity}}.\\
\textbf{متدها:}
\begin{itemize}
    \item اکسسورهای رکورد: \lr{\texttt{productId()}}, \lr{\texttt{productName()}}, \lr{\texttt{unitPrice()}}, \lr{\texttt{quantity()}}.
    \item \lr{\texttt{public Money getLineTotal()}}: محاسبه قیمت کل سطر حاصل ضرب قیمت در تعداد (\lr{Observer}).
    \item \lr{\texttt{public CartItem withQuantity(Quantity newQuantity)}}: تولید نسخه جدید با تعداد تغییریافته (\lr{Producer}).
\end{itemize}

% --- 4.7 Cart ---
\subsection{کلاس \lr{Cart} \lr{(Entity / Aggregate Root)}}
\textbf{هدف:} نگهداری سبد خرید مشتری به صورت ساختار جهش‌پذیر در طول نشست کاری.\\
\textbf{فیلدها:}
\begin{itemize}
    \item \lr{\texttt{private final String cartId}}: شناسه سبد خرید (همنام با شناسه کاربر).
    \item \lr{\texttt{private final String userId}}: شناسه کاربر مالک.
    \item \lr{\texttt{private final Map<String, CartItem> items}}: اقلام سبد خرید بر مبنای شناسه کالا.
\end{itemize}
\textbf{متدها:}
\begin{itemize}
    \item اکسسورها: \lr{\texttt{getCartId()}}, \lr{\texttt{getUserId()}}.
    \item \lr{\texttt{public void addProduct(Product product, Quantity quantity)}}: افزودن یا افزایش تعداد کالا (\lr{Mutator}).
    \item \lr{\texttt{public void updateQuantity(String productId, Quantity newQuantity)}}: اصلاح تعداد (\lr{Mutator}).
    \item \lr{\texttt{public void removeProduct(String productId)}}: حذف سطر کالا (\lr{Mutator}).
    \item \lr{\texttt{public void removeCheckedOutItems(List<CartItem> checkedOutItems)}}: حذف انتخابی اقلام پرداخت‌شده (\lr{Mutator}).
    \item \lr{\texttt{public void clear()}}: تخلیه کامل سبد خرید (\lr{Mutator}).
    \item \lr{\texttt{public int quantityOf(String productId)}}: تعداد کالای موجود در سبد (\lr{Observer}).
    \item \lr{\texttt{public Money calculateSubtotal()}}: جمع مبالغ اقلام سبد خرید (\lr{Observer}).
    \item \lr{\texttt{public boolean isEmpty()}}: بررسی خالی بودن سبد (\lr{Observer}).
    \item \lr{\texttt{public List<CartItem> getItems()}}: بازگرداندن کپی تغییرناپذیر سطرهای سبد (\lr{Observer}).
\end{itemize}

% --- 4.8 OrderItem ---
\subsection{کلاس \lr{OrderItem} \lr{(Value Object --- Record)}}
\textbf{هدف:} ثبت تغییرناپذیر اقلام در لحظه نهایی شدن سفارش (\lr{Snapshot}).\\
\textbf{فیلدها و مؤلفه‌ها:} \lr{\texttt{productId: String}}, \lr{\texttt{productName: String}}, \lr{\texttt{unitPrice: Money}}, \lr{\texttt{quantity: Quantity}}, \lr{\texttt{lineTotal: Money}}.\\
\textbf{متدها:} اکسسورهای رکورد: \lr{\texttt{productId()}}, \lr{\texttt{productName()}}, \lr{\texttt{unitPrice()}}, \lr{\texttt{quantity()}}, \lr{\texttt{lineTotal()}}.\\
\textbf{نامتغیر نمایش (\lr{RI}):} \lr{\texttt{lineTotal.equals(unitPrice.multiply(quantity))}}.

% --- 4.9 Order ---
\subsection{کلاس \lr{Order} \lr{(Entity / Aggregate Root)}}
\textbf{هدف:} سند رسمی سفارش مشتری به همراه کلیه اقلام، هزینه‌ها، نشانی و وضعیت چرخه حیات.\\
\textbf{فیلدها:}
\begin{itemize}
    \item \lr{\texttt{private final String orderId}}: شناسه یکتای سفارش.
    \item \lr{\texttt{private final String userId}}: شناسه مشتری ثبت‌کننده.
    \item \lr{\texttt{private final List<OrderItem> items}}: سطرهای قطعی اقلام سفارش.
    \item \lr{\texttt{private final Address deliveryAddress}}: نشانی تحویل سفارش.
    \item \lr{\texttt{private final Money itemsSubtotal}}: مجموع قیمت اقلام.
    \item \lr{\texttt{private final Money discountAmount}}: مبلغ تخفیف اعمال‌شده کوپن.
    \item \lr{\texttt{private final Money shippingFee}}: هزینه حمل و نقل.
    \item \lr{\texttt{private final Money payableAmount}}: مبلغ نهایی قابل پرداخت.
    \item \lr{\texttt{private OrderStatus status}}: وضعیت چرخه عمر سفارش.
    \item \lr{\texttt{private final LocalDateTime createdAt}}: تاریخ و ساعت ثبت سفارش.
    \item \lr{\texttt{private LocalDateTime paidAt}}: تاریخ و ساعت پرداخت موفق.
\end{itemize}
\textbf{متدها:}
\begin{itemize}
    \item اکسسورها: \lr{\texttt{getOrderId()}}, \lr{\texttt{getUserId()}}, \lr{\texttt{getStatus()}}, \lr{\texttt{getItemsSubtotal()}}, \lr{\texttt{getDiscountAmount()}}, \lr{\texttt{getShippingFee()}}, \lr{\texttt{getPayableAmount()}}, \lr{\texttt{getItems()}}, \lr{\texttt{getDeliveryAddress()}}, \lr{\texttt{getCreatedAt()}}, \lr{\texttt{getPaidAt()}}.
    \item \lr{\texttt{public void markAsPaid(LocalDateTime paymentTime)}}: ثبت وضعیت پرداخت موفق (\lr{Mutator}).
    \item \lr{\texttt{public void markAsPaymentFailed()}}: ثبت شکست در پرداخت با حفظ قابلیت تلاش دوباره (\lr{Mutator}).
    \item \lr{\texttt{public void updateStatus(OrderStatus newStatus)}}: تغییر وضعیت توسط مدیر یا انباردار (\lr{Mutator}).
    \item \lr{\texttt{public void cancel()}}: لغو سفارش (\lr{Mutator}).
    \item \lr{\texttt{public boolean canBeCancelled()}}: بررسی امکان‌پذیری لغو سفارش (\lr{Observer}).
\end{itemize}

% --- 4.10 PaymentTransaction ---
\subsection{کلاس \lr{PaymentTransaction} \lr{(Value Object --- Record)}}
\textbf{هدف:} ثبت قطعی تراکنش مالی و پرداخت سفارش (\lr{DS 08, US 24, US 25}).\\
\textbf{فیلدها و مؤلفه‌ها:}
\begin{itemize}
    \item \lr{\texttt{private final String transactionId}}: شناسه پیگیری یکتای تراکنش.
    \item \lr{\texttt{private final String orderId}}: شناسه سفارش مرتبط.
    \item \lr{\texttt{private final Money amount}}: مبلغ پرداختی.
    \item \lr{\texttt{private final PaymentMethod method}}: روش پرداخت انتخابی.
    \item \lr{\texttt{private final PaymentStatus status}}: نتیجه پردازش تراکنش.
    \item \lr{\texttt{private final LocalDateTime timestamp}}: زمان دقیق انجام تراکنش.
    \item \lr{\texttt{private final String referenceCode}}: کد ارجاع درگاه یا بانک.
\end{itemize}
\textbf{متدها:} اکسسورهای رکورد: \lr{\texttt{transactionId()}}, \lr{\texttt{orderId()}}, \lr{\texttt{amount()}}, \lr{\texttt{method()}}, \lr{\texttt{status()}}, \lr{\texttt{timestamp()}}, \lr{\texttt{referenceCode()}}.\\
\textbf{نامتغیر نمایش (\lr{RI}):} \lr{\texttt{transactionId != null \&\& orderId != null \&\& amount != null \&\& status != null}}.

% --- 4.11 Category ---
\subsection{کلاس \lr{Category} \lr{(Value Object / Entity)}}
\textbf{هدف:} دسته‌بندی موضوعی کالاها در ساختار کاتالوگ فروشگاه آنلاین.\\
\textbf{فیلدها:} \lr{\texttt{id: String}}, \lr{\texttt{name: String}}, \lr{\texttt{description: String}}, \lr{\texttt{active: boolean}}.\\
\textbf{متدها:} اکسسورهای استاندارد خواندنی \lr{\texttt{id()}}, \lr{\texttt{name()}}, \lr{\texttt{description()}}, \lr{\texttt{active()}}.

% --- 4.12 Coupon ---
\subsection{کلاس \lr{Coupon} \lr{(Value Object --- Record)}}
\textbf{هدف:} اعمال تخفیف‌های درصدی یا سقفی در مرحله تسویه‌حساب سفارش (\lr{US 22, DS 07}).\\
\textbf{فیلدها:} \lr{\texttt{code: String}}, \lr{\texttt{discountPercent: int}}, \lr{\texttt{maxDiscount: Money}}, \lr{\texttt{minOrderAmount: Money}}, \lr{\texttt{expiryDate: LocalDate}}.\\
\textbf{متدها:}
\begin{itemize}
    \item \lr{\texttt{public Money calculateDiscount(Money subtotal)}}: محاسبه مبلغ تخفیف با رعایت سقف مجاز (\lr{Observer}).
    \item \lr{\texttt{public boolean isValid(Money subtotal, LocalDate currentDate)}}: اعتبارسنجی انقضا و حداقل خرید (\lr{Observer}).
\end{itemize}

% --- 4.13 Review ---
\subsection{کلاس \lr{Review} \lr{(Value Object --- Record)}}
\textbf{هدف:} ثبت نظر و امتیاز کیفی خریداران درباره محصولات خریداری‌شده (\lr{US 30--32, DS 10}).\\
\textbf{فیلدها و مؤلفه‌ها:} \lr{\texttt{reviewId: String}}, \lr{\texttt{userId: String}}, \lr{\texttt{authorName: String}}, \lr{\texttt{productId: String}}, \lr{\texttt{rating: int (1..5)}}, \lr{\texttt{commentText: String}}, \lr{\texttt{createdAt: LocalDateTime}}.\\
\textbf{متدها:} اکسسورهای رکورد: \lr{\texttt{reviewId()}}, \lr{\texttt{userId()}}, \lr{\texttt{authorName()}}, \lr{\texttt{productId()}}, \lr{\texttt{rating()}}, \lr{\texttt{commentText()}}, \lr{\texttt{createdAt()}}.\\
\textbf{نامتغیر نمایش (\lr{RI}):} \lr{\texttt{rating >= 1 \&\& rating <= 5 \&\& commentText != null \&\& !commentText.isBlank() \&\& commentText.length() <= 2000}}.

% --- 4.14 ContactInfo ---
\subsection{کلاس \lr{ContactInfo} \lr{(Value Object --- Record)}}
\textbf{هدف:} کپسوله‌سازی تغییرناپذیر اطلاعات تماس کاربر (ایمیل و شماره تلفن همراه) با اعتبارسنجی عبارات باقاعده.\\
\textbf{فیلدها و مؤلفه‌ها:}
\begin{itemize}
    \item \lr{\texttt{private final String email}}: آدرس ایمیل استاندارد مطابق با الگوی \lr{RFC}.
    \item \lr{\texttt{private final String phone}}: شماره تلفن همراه ۱۱ رقمی ایران شروع با \lr{\texttt{09}}.
\end{itemize}
\textbf{سازنده:}
\begin{itemize}
    \item \lr{\texttt{public ContactInfo(String email, String phone)}}: اعتبارسنجی حضور حداقل یک مشخصه ارتباطی و انطباق مقادیر با الگوهای اعتبارسنجی.
\end{itemize}
\textbf{متدها:}
\begin{itemize}
    \item \lr{\texttt{public String email()}}, \lr{\texttt{public String phone()}}: دریافت مقادیر مؤلفه‌ها.
    \item \lr{\texttt{public boolean hasEmail()}}, \lr{\texttt{public boolean hasPhone()}}: بررسی حضور هر مؤلفه (\lr{Observers}).
\end{itemize}
\textbf{نامتغیر نمایش (\lr{RI}):} حداقل یکی از فیلدهای \lr{\texttt{email}} یا \lr{\texttt{phone}} مقدار غیرتهی داشته و مقادیر با عبارات باقاعده مطابقت دارند.\\
\textbf{تابع انتزاع (\lr{AF}):} \lr{\texttt{AF(c) = a contact channel pair with email c.email and phone c.phone}}.
\begin{contractbox}
\begin{latin}
\setlength{\parindent}{0pt}
\setlength{\parskip}{2pt}
\small
\textbf{\color{autnavy}\texttt{Constructor(String email, String phone):}} \\
\hspace*{1.5em}\textbf{\color{darkred}\texttt{REQUIRES:}} \texttt{at least one of email or phone is non-null/non-blank, and valid format} \\
\hspace*{1.5em}\textbf{\color{autblue}\texttt{MODIFIES:}} \texttt{this} \\
\hspace*{1.5em}\textbf{\color{autteal}\texttt{EFFECTS:}}  \texttt{Validates RFC email and Iranian mobile patterns; throws IllegalArgumentException if both missing or invalid.}
\end{latin}
\end{contractbox}

% --- 4.15 Enums ---
\subsection{انواع شمارشی مدل دامنه \lr{(Domain Enumerations)}}
\begin{enumerate}
    \item \textbf{\lr{UserRole}:} نقش‌های سطح دسترسی سامانه: \lr{\texttt{CUSTOMER, ADMIN, STOREKEEPER}}.
    \item \textbf{\lr{OrderStatus}:} مراحل چرخه حیات سفارش: \lr{\texttt{PENDING, PAYMENT\_FAILED, PAID, PROCESSING, SHIPPED, DELIVERED, CANCELLED}}.
    \item \textbf{\lr{PaymentMethod}:} روش‌های پرداخت مجاز: \lr{\texttt{ONLINE\_GATEWAY, WALLET, CASH\_ON\_DELIVERY}}.
    \item \textbf{\lr{PaymentStatus}:} وضعیت نتیجه تراکنش پرداخت: \lr{\texttt{SUCCESS, FAILED, CANCELLED}}.
\end{enumerate}

% =========================================================================
% SECTION 5
% =========================================================================
\newpage
\section{کاتالوگ جامع کلاس‌های لایه سرویس \lr{(Application \& Domain Services)}}

بر اساس اسلاید ۴ سند پروژه، لایه \lr{Service} هماهنگ‌کننده سناریوهای \lr{Use Case} بین چند موجودیت است و وظیفه مدیریت داده‌های درون‌حافظه‌ای (\lr{In-Memory Storage}) را بدون نیاز به لایه دیتابیس خارجی بر عهده دارد.

\begin{figure}[H]
\centering
\includegraphics[width=0.92\textwidth]{assets/figures/fig_2.png}
\caption{کلاس‌های لایه سرویس و ارتباطات هماهنگ‌سازی آنها}
\label{fig:service_layer}
\end{figure}

% --- 5.1 AuthService ---
\subsection{\lr{AuthService}}
\textbf{مسئولیت:} مدیریت ثبت‌نام، نشست‌های کاری، احراز هویت و بازیابی رمز عبور شبیه‌سازی‌شده (\lr{DS 01, DS 02, DS 03}).\\
\textbf{فیلدهای ذخیره‌سازی:}
\begin{itemize}
    \item \lr{\texttt{private final Map<String, User> userStore}}: نگاشت ایمیل/شناسه به شیء کاربر.
    \item \lr{\texttt{private final Map<String, String> activeSessions}}: نگاشت توکن نشست به شناسه کاربر.
    \item \lr{\texttt{private final Map<String, PasswordReset> passwordResetStore}}: نگاشت توکن بازیابی رمز عبور.
\end{itemize}
\begin{contractbox}
\begin{latin}
\setlength{\parindent}{0pt}
\setlength{\parskip}{2pt}
\small
\textbf{\color{autnavy}\texttt{register(String fullName, String email, String phone, String password):}} \\
\hspace*{1.5em}\textbf{\color{darkred}\texttt{REQUIRES:}} \texttt{valid email or phone, unique across system, password length >= 6.} \\
\hspace*{1.5em}\textbf{\color{autblue}\texttt{MODIFIES:}} \texttt{userStore} \\
\hspace*{1.5em}\textbf{\color{autteal}\texttt{EFFECTS:}}  \texttt{Creates User with hashed password, stores in userStore, returns User. Throws DuplicateUserException if exists.}

\vspace{4pt}\hrule\vspace{4pt}

\textbf{\color{autnavy}\texttt{login(String identifier, String password):}} \\
\hspace*{1.5em}\textbf{\color{darkred}\texttt{REQUIRES:}} \texttt{identifier != null, password != null} \\
\hspace*{1.5em}\textbf{\color{autblue}\texttt{MODIFIES:}} \texttt{activeSessions} \\
\hspace*{1.5em}\textbf{\color{autteal}\texttt{EFFECTS:}}  \texttt{Authenticates via email or phone; generates session token; throws AuthenticationException if invalid.}

\vspace{4pt}\hrule\vspace{4pt}

\textbf{\color{autnavy}\texttt{requestPasswordReset(String identifier):}} \\
\hspace*{1.5em}\textbf{\color{darkred}\texttt{REQUIRES:}} \texttt{user with identifier exists} \\
\hspace*{1.5em}\textbf{\color{autblue}\texttt{MODIFIES:}} \texttt{passwordResetStore} \\
\hspace*{1.5em}\textbf{\color{autteal}\texttt{EFFECTS:}}  \texttt{Generates 6-digit verification code valid for 10 minutes.}

\vspace{4pt}\hrule\vspace{4pt}

\textbf{\color{autnavy}\texttt{resetPassword(String code, String newPassword):}} \\
\hspace*{1.5em}\textbf{\color{darkred}\texttt{REQUIRES:}} \texttt{code is valid and non-expired} \\
\hspace*{1.5em}\textbf{\color{autblue}\texttt{MODIFIES:}} \texttt{userStore, activeSessions, passwordResetStore} \\
\hspace*{1.5em}\textbf{\color{autteal}\texttt{EFFECTS:}}  \texttt{Updates password hash, removes reset code, invalidates all existing active sessions of user.}
\end{latin}
\end{contractbox}

% --- 5.2 UserService ---
\subsection{\lr{UserService}}
\textbf{مسئولیت:} مدیریت اطلاعات پروفایل مشتری و دفترچه نشانی‌ها (\lr{DS 04, US 05, US 06, US 07}).\\
\textbf{فیلدهای ذخیره‌سازی:} \lr{\texttt{private final Map<String, User> userStore}}.\\
\begin{contractbox}
\begin{latin}
\setlength{\parindent}{0pt}
\setlength{\parskip}{2pt}
\small
\textbf{\color{autnavy}\texttt{updateProfile(String userId, String fullName, String phone):}} \\
\hspace*{1.5em}\textbf{\color{darkred}\texttt{REQUIRES:}} \texttt{user exists, fullName non-blank, phone unique across users} \\
\hspace*{1.5em}\textbf{\color{autblue}\texttt{MODIFIES:}} \texttt{userStore} \\
\hspace*{1.5em}\textbf{\color{autteal}\texttt{EFFECTS:}}  \texttt{Updates profile data; throws DuplicateUserException if phone taken by another user.}

\vspace{4pt}\hrule\vspace{4pt}

\textbf{\color{autnavy}\texttt{updateAddress(String userId, Address address):}} \\
\hspace*{1.5em}\textbf{\color{darkred}\texttt{REQUIRES:}} \texttt{user exists, address != null and addressId exists} \\
\hspace*{1.5em}\textbf{\color{autblue}\texttt{MODIFIES:}} \texttt{userStore} \\
\hspace*{1.5em}\textbf{\color{autteal}\texttt{EFFECTS:}}  \texttt{Replaces existing address in user's address book.}
\end{latin}
\end{contractbox}

% --- 5.3 ProductService ---
\subsection{\lr{ProductService}}
\textbf{مسئولیت:} مدیریت کاتالوگ محصولات، جستجو، فیلتر، مرتب‌سازی و انبارداری (\lr{DS 05, DS 11, DS 12}).\\
\textbf{فیلدهای ذخیره‌سازی:}
\begin{itemize}
    \item \lr{\texttt{private final Map<String, Product> productStore}}: نگاشت شناسه کالا به شیء محصول.
    \item \lr{\texttt{private final Map<String, Category> categoryStore}}: نگاشت دسته‌بندی‌ها.
\end{itemize}
\begin{contractbox}
\begin{latin}
\setlength{\parindent}{0pt}
\setlength{\parskip}{2pt}
\small
\textbf{\color{autnavy}\texttt{searchProducts(String query):}} \\
\hspace*{1.5em}\textbf{\color{darkred}\texttt{REQUIRES:}} \texttt{query != null and length between 3 and 100 characters} \\
\hspace*{1.5em}\textbf{\color{autblue}\texttt{MODIFIES:}} \texttt{nothing} \\
\hspace*{1.5em}\textbf{\color{autteal}\texttt{EFFECTS:}}  \texttt{Returns active products in active categories matching query (case-insensitive).}

\vspace{4pt}\hrule\vspace{4pt}

\textbf{\color{autnavy}\texttt{filterAndSort(String catId, Money min, Money max, String sort):}} \\
\hspace*{1.5em}\textbf{\color{darkred}\texttt{REQUIRES:}} \texttt{sort in \{"price\_asc", "price\_desc", "rating", "newest", "popular"\}} \\
\hspace*{1.5em}\textbf{\color{autblue}\texttt{MODIFIES:}} \texttt{nothing} \\
\hspace*{1.5em}\textbf{\color{autteal}\texttt{EFFECTS:}}  \texttt{Filters by category and price range, then sorts accordingly.}
\end{latin}
\end{contractbox}

% --- 5.4 CartService ---
\subsection{\lr{CartService}}
\textbf{مسئولیت:} هماهنگی سبد خرید مشتری با کاتالوگ محصولات (\lr{DS 06}).\\
\textbf{فیلدهای ذخیره‌سازی:}
\begin{itemize}
    \item \lr{\texttt{private final Map<String, Cart> cartStore}}: نگاشت شناسه کاربر به سبد خرید.
    \item \lr{\texttt{private final ProductService productService}}: سرویس کاتالوگ جهت بررسی موجودی.
\end{itemize}
\begin{contractbox}
\begin{latin}
\setlength{\parindent}{0pt}
\setlength{\parskip}{2pt}
\small
\textbf{\color{autnavy}\texttt{removeCheckedOutItems(String userId, List<CartItem> checkedOutItems):}} \\
\hspace*{1.5em}\textbf{\color{darkred}\texttt{REQUIRES:}} \texttt{userId != null, checkedOutItems != null} \\
\hspace*{1.5em}\textbf{\color{autblue}\texttt{MODIFIES:}} \texttt{cartStore} \\
\hspace*{1.5em}\textbf{\color{autteal}\texttt{EFFECTS:}}  \texttt{Removes from Cart only those items that were checked out and unchanged since checkout.}
\end{latin}
\end{contractbox}

% --- 5.5 OrderService ---
\subsection{\lr{OrderService}}
\textbf{مسئولیت:} تسویه‌حساب، اعمال کوپن، پرداخت، ثبت فاکتور و لغو سفارش (\lr{DS 07, DS 08, DS 09, DS 12}).\\
\textbf{فیلدهای ذخیره‌سازی:}
\begin{itemize}
    \item \lr{\texttt{private final Map<String, Order> orderStore}}: نگاشت شناسه سفارش به شیء سفارش.
    \item \lr{\texttt{private final Map<String, Coupon> couponStore}}: مخزن کدهای تخفیف معتبر.
    \item \lr{\texttt{private final CartService cartService}}: هماهنگی با سبد خرید.
    \item \lr{\texttt{private final ProductService productService}}: هماهنگی با انبار کالاها.
\end{itemize}
\begin{contractbox}
\begin{latin}
\setlength{\parindent}{0pt}
\setlength{\parskip}{2pt}
\small
\textbf{\color{autnavy}\texttt{processPayment(String orderId, String userId, PaymentMethod method, boolean simulateSuccess):}} \\
\hspace*{1.5em}\textbf{\color{darkred}\texttt{REQUIRES:}} \texttt{order belongs to userId and status is PENDING or PAYMENT\_FAILED} \\
\hspace*{1.5em}\textbf{\color{autblue}\texttt{MODIFIES:}} \texttt{orderStore, productService, cartService} \\
\hspace*{1.5em}\textbf{\color{autteal}\texttt{EFFECTS:}}  \texttt{Validates stock; if success: deducts stock, increments soldCount, marks PAID, cleans checked-out items from cart; if failure: marks PAYMENT\_FAILED.}

\vspace{4pt}\hrule\vspace{4pt}

\textbf{\color{autnavy}\texttt{cancelOrder(String orderId, String userId):}} \\
\hspace*{1.5em}\textbf{\color{darkred}\texttt{REQUIRES:}} \texttt{order belongs to userId and canBeCancelled() is true} \\
\hspace*{1.5em}\textbf{\color{autblue}\texttt{MODIFIES:}} \texttt{orderStore, productService} \\
\hspace*{1.5em}\textbf{\color{autteal}\texttt{EFFECTS:}}  \texttt{Cancels order; if previously PAID, restocks inventory in productService.}
\end{latin}
\end{contractbox}

% --- 5.6 ReviewService ---
\subsection{\lr{ReviewService}}
\textbf{مسئولیت:} ثبت و بازیابی نظرات و امتیازات محصولات با اعتبارسنجی خرید کاربر (\lr{DS 10, US 30--32}).\\
\textbf{فیلدهای ذخیره‌سازی:}
\begin{itemize}
    \item \lr{\texttt{private final Map<String, List<Review>> productReviewsStore}}: نگاشت شناسه کالا به لیست نظرات.
    \item \lr{\texttt{private final OrderService orderService}}: اعتبارسنجی پیش‌خرید کالا توسط کاربر.
\end{itemize}
\begin{contractbox}
\begin{latin}
\setlength{\parindent}{0pt}
\setlength{\parskip}{2pt}
\small
\textbf{\color{autnavy}\texttt{addReview(String userId, String authorName, String productId, int rating, String comment):}} \\
\hspace*{1.5em}\textbf{\color{darkred}\texttt{REQUIRES:}} \texttt{rating between 1 and 5, comment non-blank <= 2000 chars, userId has purchased product in PAID order} \\
\hspace*{1.5em}\textbf{\color{autblue}\texttt{MODIFIES:}} \texttt{productReviewsStore, productService} \\
\hspace*{1.5em}\textbf{\color{autteal}\texttt{EFFECTS:}}  \texttt{Creates Review, adds to store, updates product rating statistics; throws UnauthorizedActionException if not purchased.}
\end{latin}
\end{contractbox}

% =========================================================================
% SECTION 6
% =========================================================================
\newpage
\section{طراحی معماری لایه \lr{Presentation (CLI Layer Specification)}}

لایه واسط کاربری متنی (\lr{CLI}) طبق اسلاید ۴ سند پروژه، تعامل کاربر از طریق ترمینال را بر عهده دارد. برای رعایت اصول \lr{Clean Code}، واسط کاربری به منوهای ماژولار زیر تقسیم شده است:

\begin{figure}[H]
\centering
\includegraphics[width=\textwidth]{assets/figures/fig_3.png}
\caption{جریان اجرای منوهای لایه \lr{CLI} و تعامل با سرویس‌ها}
\label{fig:cli_flow}
\end{figure}

\subsection{اجزای اصلی لایه \lr{CLI}}
\begin{enumerate}
    \item \textbf{\lr{ConsoleIO}:} کلاس کمکی ورودی و خروجی استاندارد، دریافت امن رشته، عدد و رمز عبور با اعتبارسنجی در مرز ورودی.
    \item \textbf{\lr{MainMenuController}:} کنترل‌کننده مرکزی هدایت بین منوی مهمان و منوهای نقش‌محور بر اساس توکن نشست فعال.
    \item \textbf{\lr{GuestMenu}:} منوی کاربر پیش از احراز هویت شامل ثبت‌نام، ورود، بازیابی رمز و مرور کاتالوگ (\lr{US 01, 02, 04, 08--14}).
    \item \textbf{\lr{CustomerMenu}:} منوی تعاملی خریدار احرازهویت‌شده شامل پروفایل، نشانی‌ها، سبد، تسویه، پرداخت، پیگیری و لغو سفارش و ثبت نظر.
    \item \textbf{\lr{AdminMenu}:} منوی پنل مدیریت سامانه با کنترل دسترسی ویژه نقش \lr{\texttt{ADMIN}} جهت مدیریت کالاها، انبار و مشاهده و فیلتر کلیه سفارش‌ها.
    \item \textbf{\lr{CatalogMenu}:} مؤلفه ماژولار و مشترک مرور و جستجوی کاتالوگ با امکان فیلتر بر اساس دسته‌بندی و بازه قیمت و مرتب‌سازی چندگانه.
\end{enumerate}

% =========================================================================
% SECTION 7
% =========================================================================
\section{مشخصات کلاس راه‌انداز اصلی \lr{(Main.java)} و ابزارهای پشتیبان}

کلاس \lr{Main} نقطه ورود به برنامه است (اسلاید ۴ سند پروژه). وظایف سه‌گانه آن عبارتند از:
\begin{enumerate}
    \item \textbf{نمونه‌سازی و تزریق وابستگی‌ها (\lr{Manual Dependency Injection}):} ایجاد مخازن درون‌حافظه‌ای و اتصال لایه‌ها.
    \item \textbf{بارگذاری داده‌های اولیه آزمایشی (\lr{Data Seeding}):} کلاس \lr{\texttt{DataSeeder}} حساب‌های کاربری زیر را به صورت پیش‌فرض بارگذاری می‌نماید:
\end{enumerate}

\begin{table}[H]
\centering
\small
\begin{tabularx}{0.85\textwidth}{|c|c|c|c|}
\hline
\rowcolor{cardbg}
نقش کاربری & ایمیل شناسه & شماره همراه & رمز عبور پیش‌فرض \\
\hline
مدیر سیستم (\lr{ADMIN}) & \lr{\texttt{admin@shop.com}} & --- & \lr{\texttt{Admin@123}} \\
\hline
مشتری نمونه (\lr{CUSTOMER}) & \lr{\texttt{customer@shop.com}} & \lr{\texttt{09121112233}} & \lr{\texttt{Pass@123}} \\
\hline
\end{tabularx}
\caption{مشخصات حساب‌های کاربری بارگذاری‌شده در زمان راه‌اندازی برنامه}
\label{tab:seeded_accounts}
\end{table}

\begin{enumerate}[resume]
    \item \textbf{ابزارهای کمکی امنیتی (\lr{PasswordUtil}):} ارائه تابع هشینگ یک‌طرفه \lr{\texttt{hashPassword}} بر مبنای الگوریتم استاندارد \lr{SHA-256}.
\end{enumerate}

% =========================================================================
% SECTION 8
% =========================================================================
\newpage
\section{نمودارهای جامع ساختاری و روابط \lr{(UML Class Diagrams)}}

\subsection{جدول جامع ارتباط‌ها و تعداد آن‌ها \lr{(Associations \& Multiplicities)}}
\begin{table}[H]
\centering
\small
\begin{tabularx}{\textwidth}{|>{\raggedleft\arraybackslash}p{2.2cm}|>{\raggedleft\arraybackslash}p{2.5cm}|>{\centering\arraybackslash}p{2.2cm}|>{\raggedright\arraybackslash}X|}
\hline
\rowcolor{cardbg}
کلاس مبدأ & کلاس مقصد & نوع و کاردینالیتی & شرح کسب‌وکاری رابطه \\
\hline
\lr{User} & \lr{Address} & $1 \rightarrow 0..*$ & هر کاربر دارای صفر یا چند نشانی است؛ حداکثر یک پیش‌فرض. \\
\hline
\lr{User} & \lr{ContactInfo} & $1 \rightarrow 1$ & اطلاعات تماس شامل ایمیل و موبایل کاربر. \\
\hline
\lr{User} & \lr{Cart} & $1 \rightarrow 0..1$ & سبد خرید در اولین استفاده ساخته می‌شود؛ مالکیت ثابت است. \\
\hline
\lr{User} & \lr{Order} & $1 \rightarrow 0..*$ & اتصال با شناسه \lr{\texttt{userId}} برای تاریخچه سفارش‌ها. \\
\hline
\lr{Cart} & \lr{CartItem} & $1 \rightarrow 0..*$ & برای هر محصول حداکثر یک قلم در سبد وجود دارد. \\
\hline
\lr{CartItem} & \lr{Product} & $* \rightarrow 1$ & نگهداری شناسه، نام و قیمت در لحظه افزودن به سبد. \\
\hline
\lr{Product} & \lr{Category} & $* \rightarrow 1$ & دسته‌بندی کالا؛ دسته‌بندی والد باید فعال باشد. \\
\hline
\lr{Order} & \lr{OrderItem} & $1 \rightarrow 1..*$ & اقلام سفارش پس از تسویه قطعی و تغییرناپذیرند. \\
\hline
\lr{Order} & \lr{PaymentTransaction} & $1 \rightarrow 0..*$ & ثبت کلیه تراکنش‌های موفق و ناموفق پرداخت سفارش. \\
\hline
\lr{Review} & \lr{User / Product} & $* \rightarrow 1$ & ثبت نظر توسط خریدار پیشین کالا در سفارش پرداخت‌شده. \\
\hline
\end{tabularx}
\caption{جدول جامع ارتباطات، نوع رابطه‌ها و تعداد در لایه مدل}
\label{tab:associations}
\end{table}

\subsection{نمودار روابط کلاس‌های لایه \lr{Model (Core Domain Model)}}
نمودار شکل~\ref{fig:domain_model} کلیه کلاس‌های لایه \lr{Model}، ویژگی‌ها، متدها، جهت روابط، چندگانگی‌ها و انواع شمارشی را بر مبنای اصل اتحاد کامل نمایش می‌دهد:

\begin{figure}[H]
\centering
\includegraphics[width=0.98\textwidth]{assets/figures/fig_4.png}
\caption{نمودار جامع کلاس‌های لایه مدل دامنه (\lr{Core Domain UML Class Diagram})}
\label{fig:domain_model}
\end{figure}

\subsection{نمودار ماشین وضعیت چرخه عمر سفارش \lr{(Order Lifecycle Statechart)}}
نمودار شکل~\ref{fig:order_lifecycle} گذارهای مجاز وضعیت سفارش (\lr{\texttt{OrderStatus}}) از زمان تسویه‌حساب تا تحویل، شکست پرداخت، تلاش دوباره یا لغو را بر اساس اسلاید ۳ سند پروژه نمایش می‌دهد:

\begin{figure}[H]
\centering
\includegraphics[width=0.68\textwidth]{assets/figures/fig_5.png}
\caption{نمودار ماشین وضعیت چرخه حیات سفارش (\lr{Order Lifecycle Statechart})}
\label{fig:order_lifecycle}
\end{figure}

% =========================================================================
% SECTION 9
% =========================================================================
\newpage
\section{اعتبارسنجی جامع مدل و ارزیابی ده‌گانه \lr{(Validation \& Compliance)}}

مطابق چک‌لیست اعتبارسنجی، تمامی ۱۰ معیار ارزیابی مدل در جدول~\ref{tab:validation} بررسی و تأیید قطعی شدند:

\begin{table}[H]
\centering
\small
\begin{tabularx}{\textwidth}{|c|>{\raggedright\arraybackslash}p{3.8cm}|>{\raggedright\arraybackslash}X|c|}
\hline
\rowcolor{cardbg}
\textbf{ردیف} & \textbf{موضوع ارزیابی} & \textbf{نحوه انطباق در این مدل} & \textbf{وضعیت} \\
\hline
۱ & \textbf{پوشش ۱۰۰٪ داستان‌های کاربری} & هر ۳۶ یوزر استوری در ماتریس ردیابی نگاشت شده و متد مشخص دارند. & \textbf{تأیید شد} \\
\hline
۲ & \textbf{پوشش ۱۲ داستان دامنه} & فرآیندهای ۱۲ داستان دامنه مستقیماً به جریان فراخوانی بین لایه‌ها تبدیل شدند. & \textbf{تأیید شد} \\
\hline
۳ & \textbf{تفکیک قطعی \lr{Entity} و \lr{VO}} & موجودیت‌ها دارای شناسه یکتا و چرخه عمر متغیرند؛ اشیاء مقداری بدون شناسه و تغییرناپذیرند. & \textbf{تأیید شد} \\
\hline
۴ & \textbf{چندگانگی و جهت روابط} & جهت تمام یال‌ها و کاردینالیتی بر روی دیاگرام و جداول مستند شده است. & \textbf{تأیید شد} \\
\hline
۵ & \textbf{امضای کامل متدها با نوع داده} & تمامی متدها با پارامترهای دارای نوع صریح و نوع بازگشتی معین مستند شدند. & \textbf{تأیید شد} \\
\hline
۶ & \textbf{استقلال مطلق لایه \lr{Model}} & کلاس‌های \lr{Model} هیچ ارجاعی به لایه‌های بالاتر ندارند. & \textbf{تأیید شد} \\
\hline
۷ & \textbf{عدم تعریف لایه دیتابیس مجزا} & ساختارهای \lr{Map} درون سرویس‌ها کپسوله شدند. & \textbf{تأیید شد} \\
\hline
۸ & \textbf{رعایت استانداردهای \lr{Java}} & پکیج‌ها با حروف کوچک، کلاس‌ها با \lr{PascalCase} و متدها با \lr{camelCase}. & \textbf{تأیید شد} \\
\hline
۹ & \textbf{رعایت اصول \lr{Clean Code}} & رعایت اصل تک‌مسئولیتی، تفکیک فرمان و پرسش، و متدهای با اندازه کنترل‌شده. & \textbf{تأیید شد} \\
\hline
۱۰ & \textbf{مستندسازی قراردادها و نامتغیرها} & درج پیش‌شرط، پس‌شرط، \lr{RI} و \lr{AF} برای انواع داده کلیدی. & \textbf{تأیید شد} \\
\hline
\end{tabularx}
\caption{جدول ارزیابی و انطباق ده‌گانه مدل با الزامات آموزشی و استانداردهای طراحی}
\label{tab:validation}
\end{table}

% =========================================================================
% SECTION 10
% =========================================================================
\section{ثبت وضعیت و آمادگی برای ورود به فاز بعدی}

با تکمیل این سند، مدل‌سازی دامنه و تدوین ساختار کلاس‌ها به طور کامل، دقیق و یکپارچه بر مبنای اصل اتحاد دیاگرام و متن تثبیت گردید. این سند به عنوان خط مبنای قطعی و مصوب پیاده‌سازی عمل خواهد کرد.

\end{document}
"""
    with open(tex_path, "w", encoding="utf-8") as f:
        f.write(content.strip() + "\n")
    print(f"Synchronized LaTeX source written successfully to: {tex_path}")

if __name__ == "__main__":
    build_tex()
