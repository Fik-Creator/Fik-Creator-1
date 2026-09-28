module.exports = function handler(req, res) {
  res.setHeader("Cache-Control", "no-store");
  const clerkPublishableKey =
    process.env.NEXT_PUBLIC_CLERK_PUBLISHABLE_KEY ||
    process.env.CLERK_PUBLISHABLE_KEY ||
    null;

  res.status(200).json({ clerkPublishableKey });
};
