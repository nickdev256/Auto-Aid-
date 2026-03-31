import nodemailer from "nodemailer";

// Create transporter
const transporter = nodemailer.createTransport({
  host: process.env.SMTP_HOST,
  port: Number(process.env.SMTP_PORT) || 587,
  secure: process.env.SMTP_PORT == 465, // true for 465, false for others
  auth: {
    user: process.env.SMTP_USER,
    pass: process.env.SMTP_PASS,
  },
});

// Optional: verify connection (good for debugging)
transporter.verify((error, success) => {
  if (error) {
    console.error("❌ SMTP connection error:", error);
  } else {
    console.log("✅ SMTP server is ready to send emails");
  }
});

export async function sendResetEmail(to, resetLink) {
  try {
    const info = await transporter.sendMail({
      from: `"AutoAid Support" <${process.env.SMTP_FROM}>`,
      to,
      subject: "🔐 Reset Your AutoAid Password",

      // Plain text fallback
      text: `Reset your password using this link: ${resetLink}`,

      // Beautiful HTML email
      html: `
        <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto;">
          <h2 style="color:#2563eb;">AutoAid Password Reset</h2>
          <p>Hello,</p>
          <p>You requested to reset your password.</p>

          <div style="margin: 20px 0;">
            <a href="${resetLink}" 
               style="
                 background-color:#2563eb;
                 color:white;
                 padding:12px 20px;
                 text-decoration:none;
                 border-radius:6px;
                 display:inline-block;
               ">
               Reset Password
            </a>
          </div>

          <p>This link will expire in <strong>30 minutes</strong>.</p>

          <p>If you did not request this, you can safely ignore this email.</p>

          <hr style="margin:30px 0;" />

          <p style="font-size:12px; color:#666;">
            AutoAid Support Team <br/>
            Kampala, Uganda
          </p>
        </div>
      `,
    });

    console.log("📧 Reset email sent:", info.messageId);
  } catch (error) {
    console.error("❌ Failed to send reset email:", error);
    throw new Error("Email sending failed");
  }
}