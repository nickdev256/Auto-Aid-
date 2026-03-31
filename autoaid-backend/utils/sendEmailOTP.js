import nodemailer from "nodemailer";

const MAIL_HOST = process.env.MAIL_HOST || "smtp.gmail.com";
const MAIL_PORT = Number(process.env.MAIL_PORT) || 587;
const MAIL_USER = process.env.MAIL_USER;
const MAIL_PASS = process.env.MAIL_PASS;
const MAIL_FROM = process.env.MAIL_FROM || MAIL_USER;

if (!MAIL_USER || !MAIL_PASS) {
  console.warn(
    "MAIL_USER or MAIL_PASS is missing. Email sending will fail until env variables are set."
  );
}

const transporter = nodemailer.createTransport({
  host: MAIL_HOST,
  port: MAIL_PORT,
  secure: MAIL_PORT === 465,
  auth: {
    user: MAIL_USER,
    pass: MAIL_PASS,
  },
});

transporter.verify((error) => {
  if (error) {
    console.error("SMTP verification failed:", error.message);
  } else {
    console.log("SMTP server is ready to send emails");
  }
});

export async function sendEmail({ to, subject, html, text }) {
  try {
    if (!to) {
      throw new Error("Recipient email is required");
    }

    if (!subject) {
      throw new Error("Email subject is required");
    }

    const mailOptions = {
      from: `"AutoAid Support" <${MAIL_FROM}>`,
      to,
      subject,
      text: text || "",
      html: html || "",
    };

    const info = await transporter.sendMail(mailOptions);
    console.log("Email sent successfully:", info.messageId, "to:", to);

    return info;
  } catch (err) {
    console.error("sendEmail error:", err);
    throw new Error("Failed to send email");
  }
}

export async function sendEmailOTP(to, otp) {
  const safeOtp = String(otp || "").trim();

  if (!safeOtp) {
    throw new Error("OTP is required");
  }

  const html = `
    <div style="font-family: Arial, sans-serif; padding: 24px; max-width: 600px; margin: auto; background: #ffffff;">
      <div style="text-align: center; padding-bottom: 12px;">
        <h2 style="color: #2563eb; margin-bottom: 8px;">AutoAid Verification</h2>
        <p style="color: #4b5563; margin: 0;">Use the code below to verify your account</p>
      </div>

      <div style="margin: 24px 0; text-align: center;">
        <div style="display: inline-block; background: #f3f4f6; padding: 16px 28px; border-radius: 10px;">
          <div style="font-size: 34px; font-weight: bold; letter-spacing: 6px; color: #111827;">
            ${safeOtp}
          </div>
        </div>
      </div>

      <p style="color: #374151; font-size: 15px; line-height: 1.6;">
        This verification code will expire in <strong>5 minutes</strong>.
      </p>

      <p style="color: #6b7280; font-size: 13px; line-height: 1.6;">
        If you did not request this code, you can safely ignore this email.
      </p>

      <hr style="margin: 24px 0; border: none; border-top: 1px solid #e5e7eb;" />

      <p style="font-size: 12px; color: #9ca3af; text-align: center;">
        AutoAid Support
      </p>
    </div>
  `;

  return sendEmail({
    to,
    subject: "Your AutoAid Verification Code",
    text: `Your AutoAid verification code is: ${safeOtp}. This code expires in 5 minutes.`,
    html,
  });
}

export async function sendResetEmail(to, resetLink) {
  const safeLink = String(resetLink || "").trim();

  if (!safeLink) {
    throw new Error("Reset link is required");
  }

  const html = `
    <div style="font-family: Arial, sans-serif; padding: 24px; max-width: 600px; margin: auto; background: #ffffff;">
      <div style="text-align: center; padding-bottom: 12px;">
        <h2 style="color: #2563eb; margin-bottom: 8px;">Reset Your AutoAid Password</h2>
        <p style="color: #4b5563; margin: 0;">We received a request to reset your password</p>
      </div>

      <p style="color: #374151; font-size: 15px; line-height: 1.6;">
        Click the button below to reset your password:
      </p>

      <div style="text-align: center; margin: 28px 0;">
        <a
          href="${safeLink}"
          style="
            display: inline-block;
            background: #2563eb;
            color: #ffffff;
            text-decoration: none;
            padding: 12px 22px;
            border-radius: 8px;
            font-weight: 600;
          "
        >
          Reset Password
        </a>
      </div>

      <p style="color: #374151; font-size: 15px; line-height: 1.6;">
        This link will expire in <strong>30 minutes</strong>.
      </p>

      <p style="color: #6b7280; font-size: 13px; line-height: 1.6;">
        If you did not request a password reset, you can safely ignore this email.
      </p>

      <p style="color: #6b7280; font-size: 13px; line-height: 1.6; word-break: break-all;">
        If the button does not work, copy and paste this link into your browser:
        <br />
        ${safeLink}
      </p>

      <hr style="margin: 24px 0; border: none; border-top: 1px solid #e5e7eb;" />

      <p style="font-size: 12px; color: #9ca3af; text-align: center;">
        AutoAid Support
      </p>
    </div>
  `;

  return sendEmail({
    to,
    subject: "Reset your AutoAid password",
    text: `Reset your AutoAid password using this link: ${safeLink}`,
    html,
  });
}

export default sendEmailOTP;