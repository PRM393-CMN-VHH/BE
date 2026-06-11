# Tài Liệu Hướng Dẫn Sử Dụng & Gọi API Tiệm Hoa Tươi (REST API)

Tài liệu này dùng để cung cấp cho lập trình viên Frontend hoặc làm nguồn dữ liệu (Context) để **Prompt AI** (Gemini, Claude, ChatGPT) sinh mã giao diện (React, Vue, Flutter, v.v.) chính xác và kết nối trực tiếp với Backend.

---

## 1. Thông Tin Chung (General Info)
- **Base URL**: `http://localhost:8080` (hoặc cấu hình tùy môi trường).
- **Content-Type**: `application/json` cho tất cả các yêu cầu gửi lên (Request Body).
- **CORS**: Đã được kích hoạt cho mọi nguồn (`@CrossOrigin(origins = "*")`), thuận tiện cho Frontend gọi trực tiếp khi phát triển.
- **Cơ chế xác thực (Authentication)**: Sử dụng **HTTP Session**. Khi người dùng đăng nhập thành công, Backend sẽ thiết lập cookie Session ID. Frontend cần bật tùy chọn gửi credentials/cookies khi gọi API (ví dụ trong axios: `{ withCredentials: true }`).

---

## 2. Danh Sách API & Chi Tiết Lời Gọi

### 2.1. Đăng Nhập & Đăng Ký (Authentication)

#### Yêu cầu gửi mã OTP đăng ký (Request OTP)
- **Method**: `POST`
- **URL**: `/register/request-otp`
- **Request Body (JSON)**:
  ```json
  {
    "email": "user@gmail.com"
  }
  ```
- **Response**:
  - `200 OK`: `{"message": "OTP sent successfully to user@gmail.com"}`
  - `400 Bad Request`: Thiếu email.

#### Đăng ký tài khoản mới (User) - Cần OTP
- **Method**: `POST`
- **URL**: `/register?otp=123456`
- **Request Parameters (Query)**: `otp` (Mã gồm 6 chữ số gửi về email)
- **Request Body (JSON)**:
  ```json
  {
    "fullName": "Nguyễn Văn A",
    "phoneNumber": "0992222222",
    "address": "456 Nguyễn Trãi, Hà Nội",
    "email": "user@gmail.com",
    "password": "mysecurepassword"
  }
  ```
- **Response**:
  - `200 OK`: Trả về đối tượng User vừa đăng ký thành công (kèm session đăng nhập tự động).
  - `400 Bad Request`: Mã OTP sai/hết hạn, số điện thoại trùng lặp hoặc thiếu thông tin.
    ```json
    { "error": "Invalid or expired OTP" }
    // Hoặc
    { "phoneNumberExist": "Phone number is already in use" }
    ```

#### Đăng nhập (User / Admin)
- **Method**: `POST`
- **URL**: `/login`
- **Request Body (JSON)**:
  ```json
  {
    "email": "user@gmail.com",
    "password": "mysecurepassword"
  }
  ```
- **Response**:
  - `200 OK` (Admin đăng nhập):
    ```json
    {
      "role": "admin",
      "redirect": "/admin/dashboard",
      "user": { "userId": 1, "fullName": "Quản trị viên", "email": "admin@gmail.com", ... }
    }
    ```
  - `200 OK` (User đăng nhập):
    ```json
    {
      "role": "user",
      "redirect": "/home",
      "user": { "userId": 2, "fullName": "Nguyễn Văn A", "email": "user@gmail.com", ... }
    }
    ```
  - `400 Bad Request`: Tài khoản bị vô hiệu hóa.
  - `401 Unauthorized`: Sai tài khoản hoặc mật khẩu.

#### Đăng xuất
- **Method**: `POST` hoặc `GET`
- **URL**: `/logout`
- **Response**:
  - `200 OK`: `{"message": "Logged out successfully"}`

#### Lấy thông tin tài khoản đang đăng nhập hiện tại (Check Session)
- **Method**: `GET`
- **URL**: `/api/users/me`
- **Response**:
  - `200 OK`: Trả về đối tượng User hiện tại (admin hoặc user).
  - `401 Unauthorized`: `{"error": "Not authenticated"}`

---

### 2.2. Hồ Sơ Cá Nhân (User Profile)

#### Lấy thông tin hồ sơ
- **Method**: `GET`
- **URL**: `/profile`
- **Response**:
  - `200 OK`: Đối tượng User đầy đủ.
  - `401 Unauthorized`: Chưa đăng nhập.

#### Cập nhật hồ sơ
- **Method**: `POST`
- **URL**: `/profile/update`
- **Request Body (JSON)**:
  ```json
  {
    "fullName": "Nguyễn Văn A Mới",
    "phoneNumber": "0992222222",
    "address": "789 Cầu Giấy, Hà Nội"
  }
  ```
- **Response**:
  - `200 OK`: Trả về đối tượng User sau khi cập nhật thành công.

---

### 2.3. Sản Phẩm Hoa & Danh Mục (Products & Categories)

#### Lấy toàn bộ sản phẩm hoa
- **Method**: `GET`
- **URL**: `/product/all-product`
- **Response**:
  - `200 OK`: Mảng JSON chứa danh sách các sản phẩm hoa.
    ```json
    [
      {
        "productId": 1,
        "productName": "Hoa hồng đỏ Đà Lạt",
        "description": "Hoa hồng đỏ tươi Đà Lạt cành dài 60cm.",
        "price": 15000.0,
        "stock": 150,
        "imageUrl": "https://...",
        "category": { "categoryId": 1, "categoryName": "Hoa Hồng" }
      }
    ]
    ```

#### Xem chi tiết sản phẩm hoa & Sản phẩm liên quan
- **Method**: `GET`
- **URL**: `/products/{id}` (ví dụ: `/products/1`)
- **Response**:
  - `200 OK`:
    ```json
    {
      "product": { "productId": 1, "productName": "Hoa hồng đỏ Đà Lạt", ... },
      "relatedProducts": [ { "productId": 2, "productName": "Hoa hồng Ecuador Hồng", ... } ]
    }
    ```

#### Xem các sản phẩm hoa theo Danh mục (Category)
- **Method**: `GET`
- **URL**: `/product/category/{categoryId}` (ví dụ: `/product/category/1`)
- **Response**:
  - `200 OK`:
    ```json
    {
      "category": { "categoryId": 1, "categoryName": "Hoa Hồng" },
      "products": [ ... ],
      "totalProducts": 3,
      "categories": [ ... ]
    }
    ```

#### Tìm kiếm sản phẩm hoa
- **Method**: `POST`
- **URL**: `/product/search`
- **Request Parameters (Form Data)**:
  - `keyword`: Từ khóa tìm kiếm (Ví dụ: `Hồng`)
- **Response**:
  - `200 OK`: Mảng JSON chứa danh sách sản phẩm khớp từ khóa.

#### Gợi ý tìm kiếm nhanh (Search Autocomplete/Suggest)
- **Method**: `GET`
- **URL**: `/api/products/suggest`
- **Request Parameters (Query)**:
  - `keyword`: Từ khóa gõ dở
- **Response**:
  - `200 OK`: Mảng rút gọn tối đa 8 gợi ý phù hợp.
    ```json
    [
      { "id": 1, "name": "Hoa hồng đỏ Đà Lạt", "imageUrl": "https://..." }
    ]
    ```

---

### 2.4. Giỏ Hàng (Cart) - Lưu Database

*Lưu ý: Giỏ hàng được quản lý lưu trong cơ sở dữ liệu (`cart_items`) liên kết với tài khoản người dùng đang đăng nhập. Frontend bắt buộc phải gửi credentials/cookies (ví dụ trong axios: `{ withCredentials: true }`) để được xác thực. Nếu chưa đăng nhập, tất cả các API giỏ hàng bên dưới sẽ trả về lỗi `401 Unauthorized`.*

#### Xem giỏ hàng hiện tại
- **Method**: `GET`
- **URL**: `/cart`
- **Response**:
  - `200 OK`:
    ```json
    {
      "cart": [
        {
          "cartItemId": 1,
          "product": { "productId": 1, "productName": "Hoa hồng đỏ Đà Lạt", "price": 15000.0, ... },
          "quantity": 10,
          "subtotal": 150000.0
        }
      ],
      "total": 150000.0,
      "account": { ... }
    }
    ```
  - `401 Unauthorized`: `{"error": "Not logged in", "redirect": "/login"}`

#### Thêm sản phẩm hoa vào giỏ
- **Method**: `POST`
- **URL**: `/cart/add`
- **Request Body (JSON) hoặc Request Parameters**:
  - `productId`: ID của sản phẩm hoa.
  - `quantity`: Số lượng muốn thêm (mặc định 1).
- **Response**:
  - `200 OK`: `{"message": "Đã thêm sản phẩm vào giỏ hàng!", "cart": [...]}`
  - `400 Bad Request`: Hết hàng trong kho, số lượng <= 0, v.v.
  - `401 Unauthorized`: `{"error": "Not logged in", "redirect": "/login"}`

#### Cập nhật số lượng sản phẩm trong giỏ
- **Method**: `POST`
- **URL**: `/cart/update`
- **Request Body (JSON) hoặc Request Parameters**:
  - `productId`: ID sản phẩm.
  - `quantity`: Số lượng mới mong muốn.
- **Response**:
  - `200 OK`: `{"message": "Đã cập nhật số lượng!", "cart": [...]}`
  - `400 Bad Request`: Số lượng <= 0 hoặc vượt quá tồn kho, v.v.
  - `401 Unauthorized`: `{"error": "Not logged in", "redirect": "/login"}`

#### Xóa sản phẩm khỏi giỏ hàng
- **Method**: `POST`
- **URL**: `/cart/remove`
- **Request Body (JSON) hoặc Request Parameters**:
  - `productId`: ID sản phẩm cần xóa.
- **Response**:
  - `200 OK`: `{"message": "Đã xóa sản phẩm khỏi giỏ hàng!", "cart": [...]}`
  - `401 Unauthorized`: `{"error": "Not logged in", "redirect": "/login"}`

#### Thông tin trang Thanh toán (Checkout Summary)
- **Method**: `GET`
- **URL**: `/cart/checkout`
- **Response**:
  - `200 OK`: Trả về `cart`, `total` và `account` (thông tin người giao hàng mặc định).
  - `401 Unauthorized`: Chưa đăng nhập (`{"error": "Not logged in", "redirect": "/login"}`).

#### Đặt hàng (Place Order)
- **Method**: `POST`
- **URL**: `/cart/place-order`
- **Request Body (JSON) hoặc Request Parameters**:
  - `paymentMethod`: Phương thức thanh toán (`COD` hoặc `VNPay`)
- **Response**:
  - `200 OK` (Chọn thanh toán `COD` - Tiền mặt khi nhận hàng):
    ```json
    {
      "message": "Đặt hàng thành công!",
      "paymentMethod": "COD",
      "redirectUrl": "/order/detail/5",
      "order": {
        "orderId": 5,
        "totalPrice": 350000.0,
        "orderStatus": "Pending",
        "paymentStatus": "Unpaid",
        "paymentMethod": "COD",
        "createdAt": "2026-06-05T19:30:00"
      }
    }
    ```
  - `200 OK` (Chọn thanh toán `VNPay` - Ví điện tử):
    ```json
    {
      "message": "Đặt hàng thành công!",
      "paymentMethod": "VNPay",
      "redirectUrl": "/payment/create?orderId=6&amount=350000",
      "order": { "orderId": 6, ... }
    }
    ```
    *Cách xử lý*: Nếu nhận được `redirectUrl` khi chọn `VNPay`, Frontend hãy chuyển hướng trình duyệt (hoặc gọi API tiếp theo) theo `redirectUrl` để lấy link VNPay Sandbox thực hiện thanh toán.

---

### 2.5. Đơn Hàng & Cổng Thanh Toán (Orders & Payments)

#### Danh sách đơn hàng của tôi (Lịch sử đơn hàng)
- **Method**: `GET`
- **URL**: `/order/my-orders`
- **Response**:
  - `200 OK`: Mảng các đơn hàng của user đang đăng nhập.

#### Chi tiết đơn hàng
- **Method**: `GET`
- **URL**: `/order/detail/{id}` (ví dụ: `/order/detail/5`)
- **Response**:
  - `200 OK`:
    ```json
    {
      "order": { "orderId": 5, "totalPrice": 350000.0, ... },
      "orderDetails": [
        {
          "orderDetailId": 12,
          "product": { "productId": 1, "productName": "Hoa hồng đỏ Đà Lạt", ... },
          "quantity": 10,
          "price": 15000.0
        }
      ]
    }
    ```

#### Thanh toán lại đơn hàng chưa thanh toán (Re-pay qua VNPay)
- **Method**: `POST`
- **URL**: `/order/pay/{id}`
- **Response**:
  - `200 OK`: Trả về URL cổng thanh toán
    ```json
    {
      "redirectUrl": "/payment/create?orderId=5&amount=150000",
      "message": "Redirect to this url to initiate VNPay payment"
    }
    ```

#### Hủy đơn hàng (Chỉ khi chưa thanh toán)
- **Method**: `POST`
- **URL**: `/order/cancel/{id}`
- **Response**:
  - `200 OK`: `{"message": "Đã hủy giao dịch của đơn hàng #5", "status": "CANCELLED"}`

#### Lịch sử giao dịch đã thanh toán thành công
- **Method**: `GET`
- **URL**: `/transaction/history`
- **Response**:
  - `200 OK`: Mảng các đơn hàng đã thanh toán thành công (`paymentStatus` là `Paid`).

#### Cổng Tạo link thanh toán VNPay Sandbox
- **Method**: `POST` hoặc `GET`
- **URL**: `/payment/create`
- **Request Parameters**:
  - `orderId` (Required): ID của đơn hàng cần thanh toán.
  - `amount` (Required): Số tiền cần thanh toán (VND).
  - `bankCode` (Optional): Mã phương thức thanh toán muốn hướng người dùng tới trực tiếp.
    - `VNPAYQR`: Mở trực tiếp trang quét mã QR thanh toán (VNPAY-QR).
    - `VNBANK`: Mở trực tiếp trang thanh toán bằng Thẻ nội địa / Tài khoản ngân hàng.
    - `INTCARD`: Mở trực tiếp trang thanh toán bằng Thẻ thanh toán quốc tế (Visa, Mastercard, v.v.).
    - *Nếu không truyền hoặc để trống*: Người dùng sẽ được đưa tới trang chọn cổng VNPay mặc định hiển thị tất cả các phương thức thanh toán.
- **Response**:
  - `200 OK`: Trả về đường link cổng thanh toán VNPay.
    ```json
    {
      "paymentUrl": "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_Amount=15000000&..."
    }
    ```
    *Frontend điều hướng người dùng tới `paymentUrl` để thanh toán bằng thẻ Test.*

#### Cổng Xử lý callback sau khi thanh toán từ VNPay
- **Method**: `GET`
- **URL**: `/payment/vnpayReturn`
- **Mô tả**: Sau khi khách hàng thanh toán thành công hoặc thất bại trên VNPay, VNPay sẽ redirect trình duyệt của khách hàng kèm các query parameters về URL này. API này sẽ xử lý cập nhật trạng thái đơn hàng thành `Paid`, tự động trừ số lượng tồn kho của hoa đơn lẻ hoặc hoa trong combo từ cơ sở dữ liệu.
- **Response**:
  - `200 OK` (Thành công): `{"status": "success", "message": "Thanh toán thành công!", ...}`
  - `400 Bad Request` (Thất bại): `{"status": "fail", "message": "Thanh toán thất bại!"}`

---

### 2.6. Quản Trị Viên (Admin APIs)

*Lưu ý: Tất cả các APIs quản trị yêu cầu Admin đăng nhập trước (Session `adminInfo` phải tồn tại và có `roleId` = 1).*

#### Đăng nhập Admin
- **Method**: `POST`
- **URL**: `/admin/login`
- **Request Body (JSON) hoặc parameters**: `email`, `password`
- **Response**:
  - `200 OK`: Trả về dữ liệu chi tiết của quản trị viên và lưu vào Session.

#### Xem Thống kê Dashboard
- **Method**: `GET`
- **URL**: `/admin/dashboard`
- **Response**:
  - `200 OK`: Trả về tổng số thành viên, sản phẩm, đơn hàng, danh sách 5 thành viên đăng ký mới nhất và số lượng đơn hàng phân loại theo từng trạng thái (PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED).

#### Lấy danh sách đơn hàng & Lọc đơn hàng (Phân trang)
- **Method**: `GET`
- **URL**: `/admin/orders`
- **Query Parameters**:
  - `email` (tùy chọn): Lọc theo email người mua
  - `status` (tùy chọn): Lọc theo trạng thái đơn hàng
  - `startDate` / `endDate` (tùy chọn, định dạng YYYY-MM-DD): Khoảng thời gian
  - `pageNo` (mặc định 1): Số trang
- **Response**:
  - `200 OK`: Đối tượng chứa danh sách `orders`, `currentPage`, `totalPage`, danh sách `statuses`.

#### Cập nhật trạng thái đơn hàng (Admin)
- **Method**: `POST`
- **URL**: `/admin/orders/update-status`
- **Request Parameters**:
  - `orderId`: ID đơn hàng
  - `status` (PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED)
- **Mô tả**: Nếu đổi trạng thái sang `CANCELLED`, Backend sẽ tự động hoàn trả số lượng sản phẩm/combo vào kho.
- **Response**:
  - `200 OK`: `{"message": "Cập nhật trạng thái thành công!", "status": "..."}`

#### Xem/Tìm kiếm sản phẩm hoa phân trang (Admin)
- **Method**: `GET`
- **URL**: `/admin/products`
- **Query Parameters**: `pageNo` (mặc định 1), `keyword` (tìm kiếm tùy chọn)
- **Response**:
  - `200 OK`: `{"products": [...], "currentPage": 1, "totalPage": 5, "categories": [...]}`

#### Thêm sản phẩm hoa mới (Admin)
- **Method**: `POST`
- **URL**: `/admin/products/add`
- **Request Body (JSON)**:
  ```json
  {
    "productName": "Hoa hồng nhung cổ",
    "description": "Loại hoa hồng cổ hương thơm quyến rũ cực kỳ bền bông.",
    "price": 29000.0,
    "stock": 100,
    "imageUrl": "https://...",
    "category": { "categoryId": 1 }
  }
  ```
- **Response**:
  - `200 OK`: Chi tiết sản phẩm hoa vừa thêm thành công.

#### Sửa thông tin sản phẩm hoa (Admin)
- **Method**: `POST`
- **URL**: `/admin/products/edit`
- **Request Body (JSON)**:
  ```json
  {
    "productId": 9,
    "productName": "Hoa hồng nhung cổ (Đã cập nhật)",
    "description": "Mô tả mới...",
    "price": 32000.0,
    "stock": 90,
    "imageUrl": "https://...",
    "category": { "categoryId": 1 }
  }
  ```
- **Response**:
  - `200 OK`: Đối tượng sản phẩm hoa sau khi sửa.

#### Xóa sản phẩm hoa (Admin)
- **Method**: `DELETE` hoặc `GET`
- **URL**: `/admin/products/delete/{id}`
- **Response**:
  - `200 OK`: `{"message": "Product deleted successfully"}`

#### Lấy danh sách sản phẩm thiết kế Combo (Admin)
- **Method**: `GET`
- **URL**: `/admin/products/combo`
- **Response**:
  - `200 OK`: Mảng các sản phẩm thuộc danh mục "Bó Hoa / Combo".

#### Xem và Chỉnh sửa thành phần của bó hoa combo (Admin)
- **Method**: `GET`
- **URL**: `/admin/products/combo/add-item/{id}` (ví dụ: `/admin/products/combo/add-item/7`)
- **Response**:
  - `200 OK`:
    ```json
    {
      "productParent": { "productId": 7, "productName": "Bó Hoa Tình Yêu ngọt ngào", ... },
      "comboItems": [
        { "id": 1, "component": { "productId": 1, "productName": "Hoa hồng đỏ Đà Lạt", ... }, "quantity": 19 }
      ],
      "productList": [ ... ] // Danh sách các loại hoa đơn lẻ có thể thêm vào combo
    }
    ```

#### Thêm hoa đơn lẻ vào bó combo hoặc Cập nhật số lượng (Admin)
- **Method**: `POST`
- **URL**: `/admin/products/combo/add-item`
- **Request Parameters**:
  - `comboId`: ID của bó hoa combo mẹ
  - `productId`: ID của hoa đơn con muốn thêm/cập nhật
  - `quantity`: Số lượng cành hoa trong bó
- **Response**:
  - `200 OK`: Trả về mảng `comboItems` mới của combo đó.

#### Xóa hoa đơn lẻ ra khỏi bó combo (Admin)
- **Method**: `DELETE` hoặc `GET`
- **URL**: `/admin/products/combo/remove-item/{id}` (ví dụ: `/admin/products/combo/remove-item/1`)
- **Response**:
  - `200 OK`: Trả về mảng `comboItems` cập nhật.

#### Xem/Tìm kiếm danh sách thành viên (Admin)
- **Method**: `GET`
- **URL**: `/admin/users`
- **Query Parameters**: `pageNo` (mặc định 1), `search` (tên tìm kiếm tùy chọn)
- **Response**:
  - `200 OK`: `{"users": [...], "currentPage": 1, "totalPage": 2}`

#### Vô hiệu hóa (Deactivate) / Kích hoạt (Activate) thành viên (Admin)
- **Method**: `POST` hoặc `GET`
- **URL**: `/admin/users/deactivate/{id}` hoặc `/admin/users/activate/{id}`
- **Response**:
  - `200 OK`: Đối tượng User vừa đổi trạng thái thành công.

---

## 3. Gợi ý Prompt AI sinh Frontend (Prompt Templates for AI Code Gen)

Khi muốn viết code giao diện, bạn có thể sao chép một trong các Prompt mẫu dưới đây cùng toàn bộ nội dung file `.md` này rồi dán vào AI để nó tự động tạo code.

### Prompt Mẫu 1: Sinh trang Danh sách Hoa & Tìm kiếm bằng React (hoặc Next.js)
```text
Tôi đang phát triển một website bán hoa tươi. Backend là Java Spring Boot hỗ trợ các REST APIs sau (Xem tài liệu đính kèm bên dưới).
Yêu cầu:
1. Viết một component React có tên `FlowerList` hiển thị danh sách các loại hoa lấy từ `/product/all-product`.
2. Component này có thanh tìm kiếm nhanh. Khi người dùng nhập từ khóa, hãy gọi `/api/products/suggest?keyword=...` để hiển thị popup gợi ý kết quả tự động hoàn thành (autocomplete suggestions).
3. Có bộ lọc danh mục hoa ở thanh bên trái (sidebar). Khi click vào danh mục, hãy gọi `/product/category/{categoryId}` để lấy danh sách hoa theo danh mục đó.
4. Mỗi thẻ hoa có nút "Thêm vào giỏ" để gọi `POST /cart/add` truyền `productId` và `quantity: 1`. Hãy bật credentials `{ withCredentials: true }` khi gọi API bằng axios để lưu session cookie.
5. Thiết kế giao diện cực kỳ hiện đại, thẩm mỹ cao (Dùng TailwindCSS và Lucide icons).
Đính kèm tài liệu API của Backend:
[DÁN NỘI DUNG TÀI LIỆU API REST Ở ĐÂY]
```

### Prompt Mẫu 2: Sinh trang Giỏ hàng & Thanh toán VNPay
```text
Tôi đang xây dựng trang Giỏ Hàng (Cart) và Thanh Toán (Checkout) cho tiệm hoa. Backend lưu trữ giỏ hàng trong Session (Xem tài liệu APIs đính kèm bên dưới).
Yêu cầu:
1. Viết một trang React/Next.js `CartPage` hiển thị danh sách hoa trong giỏ lấy từ `GET /cart`.
2. Cho phép người dùng bấm nút tăng/giảm số lượng để gửi request `POST /cart/update` (truyền `productId` và `quantity`).
3. Cho phép xóa sản phẩm khỏi giỏ qua `POST /cart/remove`.
4. Trang checkout hiển thị tổng tiền, thông tin tài khoản mua hàng lấy từ `GET /cart/checkout`. Người dùng có thể chọn 2 hình thức: "Tiền mặt (COD)" hoặc "Ví điện tử VNPay".
5. Khi người mua bấm "Đặt hàng", gọi API `POST /cart/place-order` với phương thức được chọn:
   - Nếu là COD, hiển thị thông báo thành công và chuyển hướng đến trang chi tiết đơn hàng `/order/detail/{orderId}`.
   - Nếu là VNPay, Backend trả về `redirectUrl` (ví dụ: `/payment/create?orderId=...`). Hãy gọi tiếp API này để nhận URL liên kết đến cổng VNPay Sandbox (`paymentUrl`) và thực hiện chuyển hướng trình duyệt của người dùng `window.location.href = paymentUrl`.
Đính kèm tài liệu API của Backend:
[DÁN NỘI DUNG TÀI LIỆU API REST Ở ĐÂY]
```

### Prompt Mẫu 3: Sinh trang Quản trị (Admin Dashboard & Combo Designer)
```text
Tôi cần viết giao diện Admin bằng React để quản trị tiệm hoa và thiết kế các bó hoa combo (Xem tài liệu APIs đính kèm bên dưới).
Yêu cầu:
1. Viết trang `AdminDashboard` hiển thị các số liệu thống kê chung (tổng số khách hàng, tổng số hoa, tổng số đơn hàng, trạng thái đơn hàng) lấy từ `GET /admin/dashboard`.
2. Tạo component `ComboDesigner` dùng để cắm hoa/thiết kế bó combo:
   - Cho phép chọn một bó hoa combo (sử dụng danh sách từ `GET /admin/products/combo`).
   - Gọi `GET /admin/products/combo/add-item/{comboId}` để lấy chi tiết danh sách cành hoa đang có sẵn trong bó và danh sách toàn bộ hoa đơn lẻ khác để thêm vào.
   - Cho phép admin thêm/cập nhật hoa vào combo bằng cách gửi `POST /admin/products/combo/add-item` với `comboId`, `productId`, và `quantity`.
   - Cho phép xóa hoa khỏi combo qua `DELETE /admin/products/combo/remove-item/{id}`.
Đính kèm tài liệu API của Backend:
[DÁN NỘI DUNG TÀI LIỆU API REST Ở ĐÂY]
```
