import nodemailer from "nodemailer";

export default async function sendEmailOTP(to, otp) {
  try {
    const transporter = nodemailer.createTransport({
      host: "smtp.gmail.com",
      port: 587,                 // ✔ FIX: Port 587 works reliably, 465 often fails
      secure: false,             // ✔ FIX: Must be false when using port 587
      auth: {
        user: process.env.MAIL_USER,
        pass: process.env.MAIL_PASS,   // ✔ Must be Gmail App Password
      },
    });

    const mailOptions = {
      from: `"AutoAID Support" <${process.env.MAIL_USER}>`,
      to,
      subject: "Your AutoAID Verification Code",
      html: `
        <div style="font-family: Arial, sans-serif; padding: 20px;">
          <h2 style="color:#0056b3;">Email Verification</h2>
          <p>Your AutoAID verification code is:</p>

          <div style="padding: 15px 25px; background: #f2f2f2; 
                      display:inline-block; border-radius: 6px;">
            <h1 style="font-size:32px; letter-spacing:4px; margin:0;">
              ${otp}
            </h1>
          </div>

          <p style="margin-top:20px;">This code expires in <strong>5 minutes</strong>.</p>
          <p style="font-size:13px; color:#888;">If you did not request this, ignore this email.</p>
        </div>
      `,
    };

    await transporter.sendMail(mailOptions);

    console.log("📩 OTP Email sent successfully →", to);

  } catch (err) {
    console.error("❌ Email OTP error:", err);
    throw new Error("Could not send OTP email");
  }
}
