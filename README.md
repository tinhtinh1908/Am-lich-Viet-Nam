# Lịch Việt

Đây là gói mã nguồn công khai của ứng dụng. Gói không chứa khóa ký, chứng thư,
cấu hình hardening bản phát hành hoặc dữ liệu build nội bộ.

## Sao lưu qua Ghi chú Xiaomi

Từ trình sửa ghi chú, chọn **Sao lưu qua Ghi chú Xiaomi**, đặt mật khẩu rồi
lưu nội dung được chia sẻ vào Xiaomi Notes. Xiaomi Notes có thể đồng bộ ghi chú
này bằng Mi Cloud. Trên máy mới, mở bản sao trong Xiaomi Notes và chọn
**Chia sẻ > Lịch Việt**, nhập lại mật khẩu và xác nhận khôi phục.

Bản sao dùng PBKDF2-HMAC-SHA256 và AES-256-GCM. Mật khẩu không được lưu trong
ứng dụng; nếu quên mật khẩu thì không thể giải mã bản sao. Việc nhập là kiểu
gộp: ghi chú cùng ngày được cập nhật, ghi chú ở ngày khác vẫn giữ nguyên.

Ứng dụng lịch Android thuần, giao diện lịch tháng lấy cảm hứng từ bố cục Xiaomi Calendar nhưng được viết mới hoàn toàn.

## Chức năng v1.8.4

- Lịch tháng với tuần bắt đầu từ thứ Hai.
- Ngày dương lớn và ngày âm nhỏ trong từng ô.
- Vuốt trái/phải hoặc dùng nút mũi tên để đổi tháng.
- Chi tiết ngày âm, Can Chi ngày/tháng/năm và các ngày lễ Việt Nam.
- Nút trở về ngày hôm nay.
- Giữ ba widget cố định: ngày 1×2, ngày 2×1 và lịch tháng 4×4.
- Ba provider là class top-level độc lập, dùng cấu hình AppWidget Android thuần để launcher nhận diện trực tiếp.
- Widget ngày dùng thẻ ngày xanh gọn; widget tháng có thanh điều hướng bo tròn và đủ 42 ô.
- Widget 1×2 và 2×1 có hệ chữ lớn, khoảng cách và thẻ ngày được căn riêng để lấp đầy khung Xiaomi/HyperOS.
- Dòng ngày âm và Can Chi trên widget ngày được tăng cỡ, dùng font medium để đọc rõ từ màn hình chính.
- Ghi chú ngoại tuyến theo từng ngày; ngày có ghi chú được đánh dấu trên lịch và widget tháng.
- Chạm thẻ thông tin ngày để thêm, sửa hoặc xóa ghi chú.
- Animation chuyển tháng dạng xếp lớp 320 ms với đường cong mượt, không làm nháy nền.
- Mỗi widget có ảnh xem trước riêng trong bảng chọn của launcher.
- Widget lịch tháng 4×4 hiển thị đủ 42 ô, ngày âm, ngày lễ và điều hướng tháng.
- Giao diện lịch tự co theo tháng 5 hoặc 6 tuần, có bảng chọn ngày nhanh.
- Dark mode tự động theo hệ thống.
- Bộ chọn ngày dạng bánh xe cuộn, đồng bộ giao diện sáng/tối.
- Thẻ chi tiết ngày được neo cố định ngay phía trên thanh điều hướng ngày.
- Ngày lễ và ghi chú trong thẻ chi tiết dùng hai vùng nội dung tách biệt, không chồng chữ.
- Hiển thị tên ngắn của ngày lễ trong ô lịch và tên đầy đủ trong thẻ thông tin âm lịch.
- Nhãn ngày lễ nổi màu trên lưới và có dấu sự kiện; thẻ chi tiết chỉ hiện tên khi ngày được chọn là ngày lễ.
- Icon lịch động kiểu Xiaomi, tự đổi số ngày 1–31 khi sang ngày mới.
- Lõi tính âm lịch UTC+7 viết bằng Rust, gọi qua JNI; Java giữ giao diện Android thuần.
- Nhận diện như một ứng dụng lịch chuẩn bằng `APP_CALENDAR`, `time/epoch` và event intents.
- Thêm/sửa sự kiện qua `CalendarContract`; sự kiện dùng chung được với lịch hệ thống khi cấp quyền.
- Không phụ thuộc metadata widget hệ thống MIUI; tương thích danh sách widget chuẩn trên Android 11+ và HyperOS.
- Hoạt động ngoại tuyến, không quảng cáo; quyền Lịch chỉ được hỏi khi người dùng thêm/sửa sự kiện.
- Native ELF và APK hỗ trợ trang nhớ 16 KB cho các thiết bị Android mới.

## Thông tin gói

- Package: `com.dtinh.lichviet`
- Min SDK: 30 (Android 11)
- Target SDK: 34
- Múi giờ âm lịch: UTC+7

Thuật toán âm lịch dùng chu kỳ sóc và kinh độ Mặt Trời cho múi giờ Việt Nam. Mã nguồn không chứa mã, tài nguyên hay chữ ký của Xiaomi.

## Build mã nguồn

Yêu cầu: JDK 17, Android SDK 34, Rust stable, Android NDK và `cargo-ndk`.

```bash
rustup target add aarch64-linux-android armv7-linux-androideabi x86_64-linux-android
cargo install cargo-ndk
gradle assembleRelease
```

Gradle tự build Rust cho `arm64-v8a`, `armeabi-v7a`, `x86_64` rồi đóng gói ứng dụng Android.
