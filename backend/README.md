# Maven module structure

```text
backend/
├─ README.md
└─ source/
   ├─ pom.xml                     # root aggregator (packaging: pom) — groupId com.my.craft, artifactId backend
   ├─ applications/                # deployable Spring Boot apps
   │  ├─ pom.xml                   # aggregator (packaging: pom) — registers: main
   │  ├─ main/                     # "Template Spring Boot application: OIDC (OAuth2 client) login"
   │  │  ├─ pom.xml                # parent: spring-boot-starter-parent 3.5.16, Java 21; depends on com.my.craft:security
   │  │  └─ src/
   │  │     ├─ main/
   │  │     │  ├─ java/com/my/craft/main/
   │  │     │  │  ├─ MainApplication.java   # @SpringBootApplication(scanBasePackages = {main, security})
   │  │     │  │  └─ web/
   │  │     │  │     └─ MeController.java
   │  │     │  └─ resources/
   │  │     │     └─ application.yml
   │  │     └─ test/java/com/my/craft/main/
   │  │        └─ MainApplicationTests.java
   │  └─ inventory-service/        # empty placeholder — not yet scaffolded or registered as a module
   └─ modules/                     # shared libraries consumed by applications/*
      ├─ pom.xml                   # aggregator (packaging: pom) — registers: security
      ├─ security/                 # shared authentication/authorization config (imported by main)
      │  ├─ pom.xml                # parent: spring-boot-starter-parent 3.5.16, Java 21
      │  └─ src/main/java/com/my/craft/security/
      │     └─ config/
      │        └─ SecurityConfig.java
      └─ data-audit-log/           # empty placeholder — not yet scaffolded or registered as a module
```

## Giải thích

- `source/pom.xml`: root Maven reactor, `packaging=pom`, khai báo 2 module con: `applications` và `modules`.
- `source/applications/`: chứa các ứng dụng Spring Boot có thể chạy được (deployable). Hiện chỉ `main` có code; `inventory-service` mới là thư mục rỗng, chưa được thêm vào danh sách `<modules>`.
  - `main`: ứng dụng mẫu minh hoạ đăng nhập OAuth2/OIDC, dùng Spring Boot 3.5.16 / Java 21. Phần cấu hình Authentication/Authorization (`SecurityConfig`) đã được tách ra module `security`; `main` chỉ khai báo dependency `com.my.craft:security` và mở rộng `scanBasePackages` trong `MainApplication` để Spring quét được các `@Bean`/`@Configuration` nằm ở package `com.my.craft.security`.
    - `web/MeController.java`: controller mẫu, đọc `Authentication` principal (`OidcUser` cho session login, `Jwt` cho Bearer token) do `security` cấu hình.
    - `resources/application.yml`: cấu hình ứng dụng (bao gồm OAuth2 client/resource-server, CORS — được `SecurityConfig` bên module `security` đọc).
- `source/modules/`: chứa các thư viện dùng chung (shared libraries) cho các `applications/*`.
  - `security`: module chứa toàn bộ cấu hình Spring Security dùng chung — OIDC login (`spring-boot-starter-oauth2-client`), JWT resource server (`spring-boot-starter-oauth2-resource-server`) và CORS. Bất kỳ `applications/*` nào muốn có Authentication/Authorization chỉ cần thêm dependency vào module này và khai báo `scanBasePackages`/`@Import` để nạp cấu hình.
  - `data-audit-log`: thư mục rỗng, chưa được scaffold hay đăng ký trong `<modules>`.

Ghi chú: `inventory-service` và `data-audit-log` vẫn là chỗ dành sẵn (placeholder) cho các module tương lai — chưa có `pom.xml`/mã nguồn và chưa được đăng ký trong `<modules>` của module cha tương ứng.
