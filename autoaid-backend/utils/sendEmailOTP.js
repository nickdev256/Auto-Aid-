import nodemailer from "nodemailer";

const transporter = nodemailer.createTransport({
  host: "smtp.gmail.com",
  port: 587,
  secure: false,
  auth: {
    user: process.env.MAIL_USER,
    pass: process.env.MAIL_PASS,
  },
});

export async function sendEmail({ to, subject, html, text }) {
  try {
    if (!to) {
      throw new Error("Recipient email is required");
    }

    const mailOptions = {
      from: `"AutoAid" <${process.env.MAIL_USER}>`,
      to,
      subject,
      text: text || "",
      html: html || "",
    };

    await transporter.sendMail(mailOptions);
    console.log("Email sent to:", to);
  } catch (err) {
    console.error("sendEmail error:", err);
    throw new Error("Failed to send email");
  }
}

export async function sendEmailOTP(to, otp) {
  const html = `
    <div style="font-family: Arial, sans-serif; padding: 20px;">
      <h2 style="color:#2563eb;">AutoAid Verification</h2>
      <p>Your verification code is:</p>
      <div style="padding: 15px 25px; background: #f2f2f2; display:inline-block; border-radius: 8px;">
        <h1 style="font-size:32px; letter-spacing:4px; margin:0; color:#111827;">
          ${otp}
        </h1>
      </div>
      <p style="margin-top:20px;">This code expires in <strong>5 minutes</strong>.</p>
      <p style="font-size:13px; color:#888;">If you did not request this, ignore this email.</p>
    </div>
  `;

  return sendEmail({
    to,
    subject: "Your AutoAid Verification Code",
    text: `Your AutoAid verification code is: ${otp}`,
    html,
  });
}

export default sendEmailOTP;