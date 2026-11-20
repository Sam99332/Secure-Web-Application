export function evaluatePasswordStrength(password = '') {
  const rules = {
    minLength: password.length >= 8,
    uppercase: /[A-Z]/.test(password),
    lowercase: /[a-z]/.test(password),
    digit: /\d/.test(password),
    special: /[^A-Za-z0-9]/.test(password)
  };
  const met = Object.values(rules).filter(Boolean).length;
  let score = 0;
  if (password.length > 0) {
    if (!rules.minLength || met <= 2) score = 1;
    else if (met <= 4) score = 2;
    else if (password.length >= 12 && met === 5) score = 4;
    else score = 3;
  }
  const labels = ['Empty', 'Weak', 'Fair', 'Strong', 'Enterprise'];
  return {
    score,
    label: labels[score],
    isAcceptable: Object.values(rules).every(Boolean),
    rules
  };
}
