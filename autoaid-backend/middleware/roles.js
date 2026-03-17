export function requireRole(...roles) {
  return (req, res, next) => {
    const role = req.user?.role;
    if (!role) return res.status(401).json({ message: "Not authenticated" });

    if (!roles.includes(role)) {
      return res.status(403).json({ message: "Forbidden" });
    }
    next();
  };
}

export const adminOnly = requireRole("admin");
export const providerOnly = requireRole("provider");
export const userOnly = requireRole("user");