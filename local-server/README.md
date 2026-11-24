# Masjid & Society Local Server Guide

এই অ্যাপটি এখন যেকোনো লোকাল সার্ভারে হোস্ট করে চালানো যাবে।

## Option 1: Any PC / Laptop / Raspberry Pi / Android Termux (Node.js)

1. Node.js ইনস্টল থাকা যেকোনো কম্পিউটার বা Termux-এ এই ফোল্ডার থেকে চালান:
   ```bash
   node server.js
   ```
   (অথবা কাস্টম পোর্ট/পিন/টোকেন দিয়ে: `PORT=3000 ADMIN_PIN=1234 API_TOKEN=mysecret node server.js`)
2. টার্মিনালে দেখানো `LAN URL` (যেমন `http://192.168.1.10:3000` বা এমুলেটরের জন্য `http://10.0.2.2:3000`) কপি করুন।
3. অ্যাপের **সেটিংস > সার্ভার ও হোস্টিং সেটিংস** থেকে **Local HTTP Server** সিলেক্ট করে URL দিন, **কানেকশন পরীক্ষা করুন** ও **সংরক্ষণ** চাপুন।
4. অ্যাডমিন হিসেবে সাইন-ইন করতে ইমেইল ও Admin PIN (`1234`) দিন।

## Option 2: Firebase Local Emulator Suite

1. Firebase CLI দিয়ে ইমুলেটর চালু করুন:
   ```bash
   firebase emulators:start --only firestore,auth
   ```
2. অ্যাপের **সেটিংস > সার্ভার ও হোস্টিং সেটিংস** থেকে **Local Emulator** সিলেক্ট করে Host (`10.0.2.2` বা LAN IP) ও Ports (`8080`, `9099`) দিন।
