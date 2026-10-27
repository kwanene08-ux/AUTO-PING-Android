# AUTO PING Android

แอป Android แบบ native สำหรับตรวจสอบ TCP connection เหมาะสำหรับทดลองบน Vivo Y17s / Android 15

## สิ่งที่ทำได้
- ตรวจสอบ TCP connect ไปยัง host:port
- โหมด OFF / KEEP-ALIVE / RESET (monitor-only) / AGGRESSIVE
- ปรับ host, port, interval และ timeout
- Foreground service พร้อม Notification

## ข้อจำกัด
- ไม่ได้เพิ่มความแรงสัญญาณมือถือ และไม่รับประกันว่าแอปอื่นจะตอบสนองเร็วขึ้น
- โหมด RESET เป็นการตรวจสอบเท่านั้น ไม่รีเซ็ตโมเด็ม/เครือข่าย Android
- Foreground service ใช้ specialUse เพื่อให้การตรวจสอบที่ผู้ใช้เริ่มเองทำงานต่อพร้อม notification; Android/ผู้ผลิตยังอาจจำกัดบริการหรือหยุดแอปได้
- ไม่ใช่การเชื่อมต่อ TCP ค้างไว้: แต่ละรอบเป็น TCP connect แล้วปิด socket
- ต้องทดสอบจริงบนเครื่องก่อนใช้งานระยะยาว

## Build APK
GitHub Actions จะสร้างไฟล์ `app-debug.apk` เมื่อ push เข้า `main` หรือสั่ง workflow ด้วยตนเอง:
1. สร้าง repository ใหม่บน GitHub ชื่อ `AUTO-PING-Android`
2. อัปโหลดไฟล์ทั้งหมดในโปรเจกต์นี้เข้า repository (รักษาโฟลเดอร์ `.github/workflows`)
3. เปิดแท็บ Actions แล้วเลือก `Build AUTO PING APK`
4. รอ workflow จบเป็นสีเขียว แล้วดาวน์โหลด artifact `AUTO-PING-debug-APK`
5. แตก ZIP artifact และติดตั้ง `app-debug.apk` บนมือถือ

## ติดตั้งบน Vivo
อนุญาตให้ติดตั้งแอปจากแหล่งที่ใช้ดาวน์โหลด APK หาก Android ขออนุญาต จากนั้นเปิด AUTO PING และกด KEEP-ALIVE แล้วกดบันทึกค่าและเริ่มทำงาน หากต้องการให้ทำงานต่อเมื่อออกจากแอป ให้คง notification ไว้และตั้งค่าแบตเตอรี่เป็นอนุญาตทำงานเบื้องหลังตามเมนูที่มีใน Funtouch OS
