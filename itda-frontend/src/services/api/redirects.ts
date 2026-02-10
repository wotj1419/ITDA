export const redirectToLanding = () => {
  window.location.assign('/');
};

export const redirectToAccessDenied = () => {
  const currentPath = `${window.location.pathname}${window.location.search}`;
  const redirect = encodeURIComponent(currentPath);
  window.location.assign(`/access-denied?redirect=${redirect}`);
};
