import mongoose from "mongoose";
import bcrypt from "bcryptjs";

const userSchema = new mongoose.Schema({
  name: String,
  email: { type: String, required: true, unique: true },
  phone: String,
  password: { type: String, required: true },
  role: { type: String, enum: ["user","provider","admin"], default: "user" },
  status: { type: String, enum: ["pending","approved","rejected"], default: "approved" },
  businessName: String,
  subscription: { type: String, enum: ["free","premium"], default: "free" },
  businessType: String,
});

// Hash password before save
userSchema.pre("save", async function(next) {
  if(!this.isModified("password")) return next();
  const salt = await bcrypt.genSalt(10);
  this.password = await bcrypt.hash(this.password, salt);
  next();
});

// Compare password
userSchema.methods.comparePassword = function(password) {
  return bcrypt.compare(password, this.password);
};

export default mongoose.model("User", userSchema);
