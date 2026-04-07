# Tài liệu Chiến lược Tổng thể Dự án

# FramePilot AI

> **Nền tảng cục bộ thích ứng theo thiết bị để chuyển truyện tranh thành video**

---

## 1. Tổng quan dự án

### 1.1. Tên dự án
**FramePilot AI**

### 1.2. Tuyên bố sản phẩm
FramePilot AI là một nền tảng **local-first** có khả năng phân tích cấu hình thiết bị người dùng, tự động đánh giá năng lực xử lý, lựa chọn pipeline phù hợp và chuyển đổi nội dung truyện tranh tĩnh thành video theo nhiều mức chất lượng khác nhau.

### 1.3. Tầm nhìn
Xây dựng một nền tảng dựng video từ truyện tranh có khả năng:
- chạy cục bộ trên thiết bị tầm trung
- tự động tối ưu theo phần cứng thực tế
- giảm tối đa yêu cầu cấu hình thủ công
- đảm bảo ổn định trong quá trình xử lý
- mở rộng được thành nền tảng media generation chuyên sâu trong tương lai

### 1.4. Mục tiêu cốt lõi
- Chuyển đổi truyện tranh hoặc storyboard thành video ngắn hoặc motion comic.
- Tự động phát hiện cấu hình thiết bị và đề xuất cấu hình render phù hợp.
- Tối ưu cho môi trường local, không phụ thuộc hạ tầng cloud.
- Thiết kế hệ thống theo hướng mô-đun để có thể thay thế engine OCR, CV, AI inference và render khi cần.

---

## 2. Bối cảnh và bài toán

### 2.1. Bài toán thị trường
Hiện nay, phần lớn nội dung truyện tranh hoặc storyboard tồn tại dưới dạng ảnh tĩnh. Việc chuyển đổi thành video thường đòi hỏi:
- nhiều công cụ rời rạc
- nhiều thao tác thủ công
- kiến thức kỹ thuật cao
- phần cứng mạnh hoặc phụ thuộc cloud GPU

Với người dùng cá nhân hoặc đội nội dung nhỏ, đây là rào cản lớn về chi phí, trải nghiệm và khả năng mở rộng.

### 2.2. Bài toán kỹ thuật
Các hệ thống tạo video bằng AI hiện nay thường gặp các vấn đề sau:
- mô hình nặng, khó chạy local
- khó xác định trước thiết bị nào xử lý được mức nào
- dễ phát sinh lỗi khi tài nguyên không đủ
- không có cơ chế fallback linh hoạt khi hệ thống quá tải
- quy trình xử lý ảnh, OCR, dựng cảnh và render thường thiếu liên kết logic

### 2.3. Hướng giải quyết
FramePilot AI giải quyết bài toán bằng cách:
- quét cấu hình thiết bị khi khởi động
- benchmark nhanh năng lực xử lý thực tế
- phân loại thiết bị thành các tier năng lực
- tự động chọn pipeline phù hợp
- giám sát tài nguyên trong thời gian thực
- hạ chất lượng một cách kiểm soát khi phát hiện nguy cơ quá tải

---

## 3. Phạm vi sản phẩm

### 3.1. Phạm vi triển khai giai đoạn đầu
Hệ thống hỗ trợ các khả năng sau:

| Hạng mục | Mô tả |
|---|---|
| Nhập dữ liệu đầu vào | Ảnh truyện tranh, tập panel, storyboard |
| Tách panel | Phát hiện và chia panel cơ bản từ ảnh trang truyện |
| OCR | Nhận diện văn bản thoại trong bong bóng thoại hoặc vùng văn bản |
| Phân tích cảnh | Gom nhóm panel thành scene và shot |
| Render motion comic | Pan, zoom, subtitle, TTS, ghép video |
| Render hybrid | Một phần panel được animate nhẹ theo preset |
| Device scan | Đọc CPU, RAM, GPU, VRAM, disk, OS |
| Benchmark mini | Đánh giá nhanh OCR, inference, encode, image processing |
| Đề xuất preset | Eco, Balanced, Quality |
| Runtime monitor | Theo dõi CPU, RAM, GPU, VRAM khi render |
| Auto fallback | Giảm hiệu ứng, độ phân giải hoặc đổi pipeline nếu quá tải |

### 3.2. Ngoài phạm vi giai đoạn đầu
Các chức năng sau chưa nằm trong phạm vi MVP:

| Hạng mục | Trạng thái |
|---|---|
| Phim hoạt hình đầy đủ nhiều phút với animation liên tục | Chưa ưu tiên |
| Character consistency nâng cao xuyên cảnh | Chưa ưu tiên |
| Cloud rendering farm | Chưa triển khai |
| Cộng tác nhiều người dùng | Chưa triển khai |
| Plugin marketplace | Chưa triển khai |

---

## 4. Giá trị cốt lõi của sản phẩm

| Giá trị | Diễn giải |
|---|---|
| Local-first | Toàn bộ xử lý cốt lõi có thể thực hiện trên máy người dùng |
| Device-aware | Hệ thống hiểu năng lực phần cứng và tự điều chỉnh |
| Stability-first | Ưu tiên hoàn thành render ổn định hơn là đẩy chất lượng quá mức |
| Creator-friendly | Người dùng có thể tạo video mà không cần tinh chỉnh sâu |
| Modular architecture | Dễ thay engine OCR, AI inference hoặc media adapter |

---

## 5. Định vị sản phẩm

### 5.1. Đối tượng người dùng
- cá nhân sáng tạo nội dung
- editor hoặc studio nhỏ
- nhóm marketing nội dung
- nhóm làm nội dung giáo dục, truyện kể, thuyết minh bằng hình ảnh

### 5.2. Giá trị khác biệt
FramePilot AI không chỉ là công cụ dựng video, mà là một nền tảng có khả năng:
- tự đánh giá cấu hình máy
- tự chọn cách xử lý phù hợp
- tự kiểm soát tài nguyên khi chạy
- giảm nguy cơ treo máy hoặc render thất bại

### 5.3. Định vị chiến lược
FramePilot AI được định vị là:
> **Nền tảng chuyển đổi truyện tranh sang video theo hướng cục bộ, thích ứng phần cứng và tối ưu trải nghiệm người dùng**

---

## 6. Kiến trúc nghiệp vụ tổng thể

### 6.1. Mục tiêu nghiệp vụ
Hệ thống phải xử lý được ba bài toán nghiệp vụ chính:

1. **Hiểu thiết bị**  
   Xác định thiết bị có thể làm gì, ở mức chất lượng nào, với độ ổn định ra sao.

2. **Hiểu nội dung truyện**  
   Phân tích panel, thoại, cảnh và timeline để xây dựng kế hoạch dựng video.

3. **Dựng video an toàn**  
   Tạo video với preset phù hợp, theo dõi tải hệ thống và giảm cấu hình khi cần.

### 6.2. Thực thể nghiệp vụ chính

| Thực thể | Mô tả |
|---|---|
| Device Profile | Hồ sơ phần cứng hiện tại của thiết bị |
| Benchmark Result | Kết quả các tác vụ benchmark ngắn |
| Capability Tier | Phân loại năng lực thiết bị |
| Project | Dự án dựng video |
| Asset | Tài nguyên đầu vào như ảnh, audio, subtitle |
| Scene | Cảnh nghiệp vụ được tạo từ panel |
| Shot | Đơn vị dựng video nhỏ trong timeline |
| Pipeline Plan | Kế hoạch chọn pipeline xử lý |
| Render Job | Công việc render cụ thể |
| Runtime Session | Phiên chạy render với dữ liệu giám sát |
| Export Artifact | Tệp video đầu ra và metadata |

---

## 7. Luồng nghiệp vụ đầu cuối

### 7.1. Luồng tổng quát
```text
Người dùng mở ứng dụng
    → Hệ thống quét cấu hình thiết bị
    → Hệ thống chạy benchmark nhanh
    → Hệ thống phân loại năng lực máy
    → Hệ thống đề xuất preset xử lý
    → Người dùng nhập truyện tranh hoặc panel
    → Hệ thống phân tích panel và OCR thoại
    → Hệ thống tạo scene và shot plan
    → Hệ thống chọn pipeline render
    → Hệ thống thực thi render có giám sát tài nguyên
    → Hệ thống xuất video hoàn chỉnh
```

### 7.2. Luồng nghiệp vụ chi tiết

| Bước | Đầu vào | Xử lý | Đầu ra |
|---|---|---|---|
| 1 | Khởi động ứng dụng | Quét phần cứng | Device Profile |
| 2 | Device Profile | Benchmark mini | Benchmark Result |
| 3 | Device + Benchmark | Chấm điểm và xếp hạng | Capability Tier |
| 4 | Asset đầu vào | Phân tích hình ảnh và thoại | Scene Map |
| 5 | Scene Map + Tier | Chọn preset và pipeline | Pipeline Plan |
| 6 | Pipeline Plan | Tạo đồ thị job render | Render Graph |
| 7 | Render Graph | Render có giám sát runtime | Output tạm |
| 8 | Output tạm | Ghép video cuối | Export Artifact |

---

## 8. Chiến lược logic xử lý

### 8.1. Logic nhận diện năng lực thiết bị
Hệ thống cần đánh giá được:
- thiết bị hiện tại có GPU hay không
- dung lượng VRAM thực tế có phù hợp cho pipeline nào
- RAM còn trống đủ cho quá trình dựng video hay không
- CPU có phù hợp cho encode hoặc inference nhẹ không
- tốc độ I/O ổ đĩa có ảnh hưởng đến quá trình xử lý hay không

### 8.2. Logic phân loại thiết bị

| Loại thiết bị | Điều kiện điển hình | Đề xuất xử lý |
|---|---|---|
| Low | Không GPU hoặc GPU yếu, RAM thấp | Motion comic |
| Medium | GPU mức phổ thông, RAM 16GB | Hybrid cơ bản |
| High | GPU khá, benchmark tốt | Hybrid nâng cao |

### 8.3. Logic chọn preset

| Preset | Mục tiêu |
|---|---|
| Eco | Ưu tiên ổn định, giảm tải hệ thống |
| Balanced | Cân bằng giữa chất lượng và hiệu năng |
| Quality | Ưu tiên chất lượng khi thiết bị đủ mạnh |

### 8.4. Logic chọn pipeline
Pipeline được chọn dựa trên:
- điểm phần cứng
- kết quả benchmark
- kích thước và độ phức tạp của project
- số lượng panel và số shot dự kiến
- nguy cơ quá tải khi render

---

## 9. Chế độ pipeline

### 9.1. MotionComic Pipeline
Áp dụng cho thiết bị yếu hoặc dự án cần render nhanh.

Đặc điểm:
- pan/zoom
- subtitle
- TTS
- chuyển cảnh đơn giản
- không dùng hoặc rất ít AI animation

### 9.2. HybridLite Pipeline
Áp dụng cho thiết bị trung bình thấp.

Đặc điểm:
- motion comic là chủ đạo
- bổ sung một số hiệu ứng dịch chuyển lớp ảnh
- animate số lượng nhỏ shot quan trọng

### 9.3. Hybrid Pipeline
Áp dụng cho thiết bị tầm trung.

Đặc điểm:
- scene planning chi tiết hơn
- nhiều shot có animate nhẹ
- kết hợp pan/zoom, parallax và hiệu ứng lựa chọn

### 9.4. Enhanced Hybrid Pipeline
Áp dụng cho thiết bị có năng lực cao hơn.

Đặc điểm:
- tỷ lệ shot có AI animation cao hơn
- hiệu ứng chuyển cảnh tốt hơn
- khả năng xuất độ phân giải cao hơn

---

## 10. Kiến trúc hệ thống

### 10.1. Kiến trúc tổng thể

| Tầng | Công nghệ | Vai trò |
|---|---|---|
| Desktop UI | Tauri + React + TypeScript | Giao diện người dùng |
| Local API | Spring Boot + WebFlux | Điều phối và API nội bộ |
| Domain Services | Java Modules | Nghiệp vụ và orchestration |
| Native AI/CV Engine | ONNX Runtime Java, JavaCV, FFmpeg | Inference, CV, render |
| Local Storage | SQLite + File System | Lưu project, cache, artifact |
| Device Monitoring | OSHI + GPU adapter | Theo dõi phần cứng và runtime |

### 10.2. Sơ đồ logic hệ thống
```text
Desktop UI
    ↓
Local API Gateway / Controller
    ↓
Application Services
    ├─ Device Intelligence
    ├─ Project Management
    ├─ Scene Planning
    ├─ Pipeline Selection
    ├─ Render Orchestration
    └─ Runtime Control
    ↓
Engine Adapters
    ├─ OCR Adapter
    ├─ ONNX Adapter
    ├─ JavaCV Adapter
    ├─ FFmpeg Adapter
    └─ TTS Adapter
    ↓
Local Storage + Export Layer
```

---

## 11. Phân rã mô-đun hệ thống

| Mô-đun | Trách nhiệm |
|---|---|
| device-scanner | Đọc CPU, RAM, GPU, VRAM, disk, OS |
| benchmark-runner | Chạy benchmark ngắn cho OCR, render, inference |
| capability-engine | Tính điểm và phân loại năng lực máy |
| preset-engine | Đề xuất preset theo điểm và ràng buộc |
| project-manager | Quản lý project và asset |
| panel-parser | Phát hiện, tách và chuẩn hóa panel |
| ocr-engine | OCR thoại và text liên quan |
| scene-planner | Phân tích panel thành scene |
| shot-planner | Chia nhỏ scene thành shot render |
| pipeline-selector | Chọn pipeline phù hợp |
| render-orchestrator | Điều phối job render |
| runtime-monitor | Theo dõi tài nguyên thời gian thực |
| fallback-engine | Tự hạ cấu hình khi cần |
| export-service | Ghép và xuất video cuối |
| audit-logger | Ghi sự kiện, log và timeline xử lý |

---

## 12. Chiến lược xử lý nội dung

### 12.1. Xử lý ảnh đầu vào
Các ảnh đầu vào cần được chuẩn hóa về:
- định dạng tệp
- kích thước tối đa
- độ phân giải làm việc
- metadata cảnh hoặc thứ tự

### 12.2. Tách panel
Mục tiêu của bước này là:
- phát hiện vùng panel trên trang truyện
- xác định thứ tự đọc
- cắt thành đơn vị panel độc lập

### 12.3. OCR và trích xuất thoại
Hệ thống cần:
- nhận diện văn bản trong bong bóng thoại hoặc khối văn bản
- gắn text theo panel
- chuẩn bị dữ liệu cho subtitle và TTS

### 12.4. Phân cảnh và tạo shot
Từ panel và thoại, hệ thống xây dựng:
- scene list
- shot list
- timeline sơ bộ
- thông tin hiệu ứng cần áp dụng

---

## 13. Chiến lược render

### 13.1. Nguyên tắc render
- Render theo shot nhỏ để giảm rủi ro.
- Cho phép resume từ checkpoint nếu lỗi.
- Theo dõi sát tài nguyên hệ thống trong khi chạy.
- Có thể hạ preset mà không hủy toàn bộ tiến trình.

### 13.2. Đồ thị công việc render
Mỗi dự án được chia thành các job:
- tiền xử lý asset
- render shot
- ghép âm thanh
- ghép subtitle
- hợp nhất video
- xuất kết quả cuối

### 13.3. Chế độ xuất đầu ra

| Chế độ | Mô tả |
|---|---|
| Preview | Render nhanh để xem thử |
| Standard Export | Bản xuất mặc định |
| Safe Export | Ưu tiên ổn định, giảm độ phân giải |
| High Fidelity Export | Chỉ áp dụng khi thiết bị đủ khả năng |

---

## 14. Chiến lược fallback

### 14.1. Điều kiện kích hoạt fallback
Fallback được kích hoạt khi:
- VRAM vượt ngưỡng an toàn
- RAM còn trống dưới ngưỡng tối thiểu
- tốc độ render thấp hơn ngưỡng kỳ vọng
- một engine bị timeout hoặc lỗi lặp lại

### 14.2. Cấp độ fallback

| Cấp độ | Hành động |
|---|---|
| F1 | Giảm hiệu ứng không cần thiết |
| F2 | Giảm độ phân giải |
| F3 | Giảm số shot animate |
| F4 | Chuyển hoàn toàn sang motion comic |

### 14.3. Mục tiêu fallback
Mục tiêu của fallback không phải là bảo toàn chất lượng tối đa, mà là:
- giữ phiên render tiếp tục chạy
- giảm nguy cơ crash
- đảm bảo tạo được video đầu ra usable

---

## 15. Chiến lược trải nghiệm người dùng

### 15.1. Nguyên tắc UX
- mặc định tự động tối đa
- cho phép override ở tầng nâng cao
- giải thích rõ vì sao hệ thống đề xuất preset đó
- hiển thị rủi ro khi người dùng ép chất lượng vượt ngưỡng

### 15.2. Các màn hình chính

| Màn hình | Chức năng |
|---|---|
| Device Scan | Quét và hiển thị cấu hình máy |
| Benchmark Summary | Hiển thị điểm benchmark |
| Project Import | Nhập và sắp xếp asset |
| Scene Review | Xem kết quả phân cảnh |
| Preset Recommendation | Đề xuất preset |
| Render Console | Theo dõi tiến trình render |
| Export Center | Quản lý file đầu ra |

---

## 16. Kiến trúc kỹ thuật khuyến nghị

### 16.1. Stack công nghệ chính thức

| Hạng mục | Công nghệ |
|---|---|
| Desktop App | Tauri + React + TypeScript |
| Styling | Tailwind CSS + shadcn/ui |
| Local Backend | Spring Boot + WebFlux |
| AI Inference | ONNX Runtime Java |
| Computer Vision / Media | JavaCV |
| Video Render | FFmpeg |
| OCR | PaddleOCR hoặc OCR model ONNX |
| Device Profiling | OSHI |
| GPU Monitoring | NVML Adapter hoặc Command Adapter |
| Database local | SQLite |
| Logging | SLF4J + Logback |
| Configuration | YAML |

### 16.2. Vai trò của từng lớp kỹ thuật
- **Tauri + React**: cung cấp giao diện desktop nhẹ, hiện đại, dễ bảo trì.
- **Spring WebFlux**: xử lý orchestration, API nội bộ, luồng job và progress stream.
- **ONNX Runtime Java**: chạy model inference cục bộ với khả năng tối ưu phần cứng tốt.
- **JavaCV**: bridge tới OpenCV/FFmpeg/native media processing.
- **FFmpeg**: lõi xuất video và xử lý audio, subtitle, muxing.
- **OSHI**: thu thập thông tin phần cứng và tình trạng runtime.

---

## 17. Cấu trúc thư mục gợi ý

```text
framepilot-ai/
├─ apps/
│  ├─ desktop-ui/
│  └─ local-server/
├─ modules/
│  ├─ device-scanner/
│  ├─ benchmark-runner/
│  ├─ capability-engine/
│  ├─ preset-engine/
│  ├─ project-manager/
│  ├─ panel-parser/
│  ├─ ocr-engine/
│  ├─ scene-planner/
│  ├─ shot-planner/
│  ├─ pipeline-selector/
│  ├─ render-orchestrator/
│  ├─ runtime-monitor/
│  ├─ fallback-engine/
│  └─ export-service/
├─ engines/
│  ├─ onnx/
│  ├─ ffmpeg/
│  ├─ javacv/
│  └─ ocr/
├─ storage/
│  ├─ projects/
│  ├─ cache/
│  ├─ exports/
│  └─ app.db
├─ configs/
│  ├─ application.yml
│  ├─ presets/
│  └─ models/
├─ docs/
│  ├─ product/
│  ├─ architecture/
│  ├─ operations/
│  └─ decisions/
└─ scripts/
```

---

## 18. Chiến lược phát triển và triển khai

### 18.1. Giai đoạn triển khai

| Giai đoạn | Mục tiêu |
|---|---|
| Discovery | Chốt phạm vi và tài liệu sản phẩm |
| Foundation | Dựng core platform, scan thiết bị, benchmark |
| MVP | Import asset, OCR, scene planning, motion comic render |
| Stabilization | Tối ưu hiệu năng, monitor, fallback |
| Expansion | Hybrid rendering và nâng cao chất lượng |

### 18.2. Phạm vi MVP khuyến nghị
- quét cấu hình thiết bị
- benchmark mini
- đề xuất preset tự động
- nhập panel hoặc chapter
- OCR thoại
- dựng motion comic end-to-end
- TTS nội bộ
- subtitle tự động
- runtime monitor
- fallback cơ bản

### 18.3. Nguyên tắc triển khai
- xây dựng theo mô-đun độc lập
- triển khai từng pipeline nhỏ trước khi mở rộng
- tập trung vào tỷ lệ render thành công ở lần đầu
- ưu tiên tính ổn định và khả năng phục hồi

---

## 19. Yêu cầu phi chức năng

| Yêu cầu | Mục tiêu |
|---|---|
| Thời gian khởi động | Nhanh |
| Độ ổn định | Cao |
| Khả năng phục hồi | Resume được sau lỗi |
| Tính quan sát được | Có log, event và metrics |
| Bảo trì | Dễ thay engine và debug |
| Tính cục bộ | Hoạt động tốt không phụ thuộc mạng |

---

## 20. Bảo mật và độ tin cậy

### 20.1. Bảo mật cơ bản
- Chỉ đọc và ghi tệp trong phạm vi project được quản lý.
- Chỉ cho phép gọi engine native nằm trong danh sách đã xác thực.
- Kiểm tra đường dẫn xuất để tránh lỗi ghi đè ngoài ý muốn.
- Ghi log theo hướng không làm lộ dữ liệu nhạy cảm.

### 20.2. Độ tin cậy
- job render có retry giới hạn
- có checkpoint để phục hồi tiến trình
- có health-check cho engine OCR và FFmpeg
- có xác minh file đầu ra sau khi export

---

## 21. Quan sát hệ thống và logging

### 21.1. Metrics cần thu thập

| Metric | Ý nghĩa |
|---|---|
| render_duration | Tổng thời gian render |
| inference_latency | Độ trễ của model |
| ocr_latency | Độ trễ OCR |
| peak_ram | RAM đỉnh |
| peak_vram | VRAM đỉnh |
| fallback_count | Số lần fallback |
| export_success_rate | Tỷ lệ export thành công |

### 21.2. Sự kiện hệ thống cần log

| Event | Mô tả |
|---|---|
| DEVICE_PROFILE_CREATED | Hoàn tất quét thiết bị |
| BENCHMARK_COMPLETED | Hoàn tất benchmark |
| PRESET_RECOMMENDED | Đã đề xuất preset |
| SCENE_PLAN_CREATED | Đã tạo kế hoạch cảnh |
| RENDER_STARTED | Bắt đầu render |
| FALLBACK_TRIGGERED | Kích hoạt fallback |
| EXPORT_COMPLETED | Hoàn tất export |

---

## 22. Rủi ro và chiến lược giảm thiểu

| Rủi ro | Mức độ | Biện pháp |
|---|---|---|
| OCR sai hoặc thiếu | Trung bình | Cho phép chỉnh sửa thủ công |
| Panel detect không chính xác | Trung bình | Thêm màn hình review panel |
| VRAM không đủ | Cao | Áp dụng fallback sớm |
| Hiệu năng máy không ổn định | Cao | Benchmark thực tế trước khi render |
| JavaCV hoặc FFmpeg integration lỗi | Trung bình | Dùng adapter layer và health check |
| Người dùng ép preset quá cao | Trung bình | Cảnh báo và khóa ngưỡng nguy hiểm |

---

## 23. Chỉ số thành công

### 23.1. Chỉ số sản phẩm

| KPI | Mục tiêu |
|---|---|
| Tỷ lệ render thành công lần đầu | Cao |
| Tỷ lệ chấp nhận preset đề xuất | Cao |
| Tỷ lệ crash | Thấp |
| Thời gian tạo video đầu tiên | Thấp |
| Mức độ hài lòng về trải nghiệm auto-config | Cao |

### 23.2. Chỉ số kỹ thuật

| KPI | Mục tiêu |
|---|---|
| Device scan latency | Thấp |
| Benchmark completion time | Ngắn |
| Fallback recovery success | Cao |
| Export success rate | Cao |
| Runtime stability under medium load | Cao |

---

## 24. Mô hình tổ chức triển khai

| Vai trò | Trách nhiệm |
|---|---|
| Product Lead | Chiến lược sản phẩm, phạm vi, ưu tiên |
| Tech Lead | Kiến trúc hệ thống tổng thể |
| Frontend Engineer | Desktop UI và trải nghiệm người dùng |
| Backend Engineer | WebFlux orchestration và domain services |
| CV/Media Engineer | OCR, image processing, FFmpeg, JavaCV |
| ML Engineer | ONNX model, tối ưu inference |
| QA/Release Engineer | Kiểm thử, đóng gói, phát hành |

---

## 25. Bộ tài liệu chuyên môn nên duy trì

| Tài liệu | Mục đích |
|---|---|
| PRD | Mô tả yêu cầu sản phẩm |
| HLD | Kiến trúc cấp cao |
| LLD | Thiết kế chi tiết mô-đun |
| ADR | Nhật ký quyết định kiến trúc |
| Benchmark Spec | Quy chuẩn benchmark thiết bị |
| Ops Runbook | Quy trình vận hành và xử lý sự cố |
| QA Matrix | Ma trận kiểm thử |
| Release Notes | Ghi nhận thay đổi theo phiên bản |

---

## 26. Kết luận chiến lược

FramePilot AI nên được xây dựng như một **nền tảng cục bộ thích ứng theo thiết bị**, thay vì chỉ là một công cụ render video đơn lẻ.

Trọng tâm chiến lược của dự án là:
- hiểu phần cứng trước khi xử lý
- ưu tiên tính ổn định hơn chất lượng cực đại
- chia pipeline thành các bước nhỏ có thể giám sát và phục hồi
- giữ kiến trúc mô-đun để dễ thay đổi engine trong tương lai

### Quyết định chiến lược cuối cùng

| Hạng mục | Quyết định |
|---|---|
| Tên dự án | FramePilot AI |
| Mô hình | Adaptive Local Comic-to-Video Platform |
| Kiến trúc | Tauri + React + Spring WebFlux + Native AI/CV Engine |
| Triết lý thiết kế | Stability-first, device-aware, local-first |
| Mục tiêu MVP | Tự động chuyển truyện tranh thành motion comic hoặc hybrid video theo cấu hình máy |

---

## 27. Phụ lục: Tuyên bố định hướng triển khai

> FramePilot AI chỉ nên xử lý trong ngưỡng mà thiết bị thực tế có thể vận hành ổn định.  
> Hệ thống phải tự động nhận biết giới hạn đó, tự đề xuất cấu hình phù hợp và tự bảo vệ tiến trình xử lý khi phát hiện nguy cơ quá tải.

