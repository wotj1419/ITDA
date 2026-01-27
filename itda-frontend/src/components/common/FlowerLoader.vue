<template>
<div class="loader">
  <svg
    id="pegtopone"
    width="100"
    height="100"
    xmlns="http://www.w3.org/2000/svg"
    xmlns:xlink="http://www.w3.org/1999/xlink"
    viewBox="0 0 100 100"
  >
    <defs>
      <filter id="shine">
        <feGaussianBlur stdDeviation="3"></feGaussianBlur>
      </filter>
      <mask id="mask">
        <path
          d="M63,37c-6.7-4-4-27-13-27s-6.3,23-13,27-27,4-27,13,20.3,9,27,13,4,27,13,27,6.3-23,13-27,27-4,27-13-20.3-9-27-13Z"
          fill="white"
        ></path>
      </mask>
      <radialGradient
        id="gradient-1"
        cx="50"
        cy="66"
        fx="50"
        fy="66"
        r="30"
        gradientTransform="translate(0 35) scale(1 0.5)"
        gradientUnits="userSpaceOnUse"
      >
        <stop offset="0%" stop-color="black" stop-opacity="0.3"></stop>
        <stop offset="50%" stop-color="black" stop-opacity="0.1"></stop>
        <stop offset="100%" stop-color="black" stop-opacity="0"></stop>
      </radialGradient>
      <radialGradient
        id="gradient-2"
        cx="55"
        cy="20"
        fx="55"
        fy="20"
        r="30"
        gradientUnits="userSpaceOnUse"
      >
        <stop offset="0%" stop-color="white" stop-opacity="0.3"></stop>
        <stop offset="50%" stop-color="white" stop-opacity="0.1"></stop>
        <stop offset="100%" stop-color="white" stop-opacity="0"></stop>
      </radialGradient>
      <radialGradient
        id="gradient-3"
        cx="85"
        cy="50"
        fx="85"
        fy="50"
        xlink:href="#gradient-2"
      ></radialGradient>
      <radialGradient
        id="gradient-4"
        cx="50"
        cy="58"
        fx="50"
        fy="58"
        r="60"
        gradientTransform="translate(0 47) scale(1 0.2)"
        xlink:href="#gradient-3"
      ></radialGradient>
      <linearGradient
        id="gradient-5"
        x1="50"
        y1="90"
        x2="50"
        y2="10"
        gradientUnits="userSpaceOnUse"
      >
        <stop offset="0%" stop-color="black" stop-opacity="0.2"></stop>
        <stop offset="40%" stop-color="black" stop-opacity="0"></stop>
      </linearGradient>
    </defs>
    <g>
      <path
        d="M63,37c-6.7-4-4-27-13-27s-6.3,23-13,27-27,4-27,13,20.3,9,27,13,4,27,13,27,6.3-23,13-27,27-4,27-13-20.3-9-27-13Z"
        fill="currentColor"
      ></path>
      <path
        d="M63,37c-6.7-4-4-27-13-27s-6.3,23-13,27-27,4-27,13,20.3,9,27,13,4,27,13,27,6.3-23,13-27,27-4,27-13-20.3-9-27-13Z"
        fill="url(#gradient-1)"
      ></path>
      <path
        d="M63,37c-6.7-4-4-27-13-27s-6.3,23-13,27-27,4-27,13,20.3,9,27,13,4,27,13,27,6.3-23,13-27,27-4,27-13-20.3-9-27-13Z"
        fill="none"
        stroke="white"
        opacity="0.3"
        stroke-width="3"
        filter="url(#shine)"
        mask="url(#mask)"
      ></path>
      <path
        d="M63,37c-6.7-4-4-27-13-27s-6.3,23-13,27-27,4-27,13,20.3,9,27,13,4,27,13,27,6.3-23,13-27,27-4,27-13-20.3-9-27-13Z"
        fill="url(#gradient-2)"
      ></path>
      <path
        d="M63,37c-6.7-4-4-27-13-27s-6.3,23-13,27-27,4-27,13,20.3,9,27,13,4,27,13,27,6.3-23,13-27,27-4,27-13-20.3-9-27-13Z"
        fill="url(#gradient-3)"
      ></path>
      <path
        d="M63,37c-6.7-4-4-27-13-27s-6.3,23-13,27-27,4-27,13,20.3,9,27,13,4,27,13,27,6.3-23,13-27,27-4,27-13-20.3-9-27-13Z"
        fill="url(#gradient-4)"
      ></path>
      <path
        d="M63,37c-6.7-4-4-27-13-27s-6.3,23-13,27-27,4-27,13,20.3,9,27,13,4,27,13,27,6.3-23,13-27,27-4,27-13-20.3-9-27-13Z"
        fill="url(#gradient-5)"
      ></path>
    </g>
  </svg>
  <svg
    id="pegtoptwo"
    width="100"
    height="100"
    xmlns="http://www.w3.org/2000/svg"
    xmlns:xlink="http://www.w3.org/1999/xlink"
    viewBox="0 0 100 100"
  >
    <defs>
      <filter id="shine">
        <feGaussianBlur stdDeviation="3"></feGaussianBlur>
      </filter>
      <mask id="mask">
        <path
          d="M63,37c-6.7-4-4-27-13-27s-6.3,23-13,27-27,4-27,13,20.3,9,27,13,4,27,13,27,6.3-23,13-27,27-4,27-13-20.3-9-27-13Z"
          fill="white"
        ></path>
      </mask>
      <radialGradient
        id="gradient-1"
        cx="50"
        cy="66"
        fx="50"
        fy="66"
        r="30"
        gradientTransform="translate(0 35) scale(1 0.5)"
        gradientUnits="userSpaceOnUse"
      >
        <stop offset="0%" stop-color="black" stop-opacity="0.3"></stop>
        <stop offset="50%" stop-color="black" stop-opacity="0.1"></stop>
        <stop offset="100%" stop-color="black" stop-opacity="0"></stop>
      </radialGradient>
      <radialGradient
        id="gradient-2"
        cx="55"
        cy="20"
        fx="55"
        fy="20"
        r="30"
        gradientUnits="userSpaceOnUse"
      >
        <stop offset="0%" stop-color="white" stop-opacity="0.3"></stop>
        <stop offset="50%" stop-color="white" stop-opacity="0.1"></stop>
        <stop offset="100%" stop-color="white" stop-opacity="0"></stop>
      </radialGradient>
       <radialGradient
        id="gradient-3"
        cx="85"
        cy="50"
        fx="85"
        fy="50"
        xlink:href="#gradient-2"
      ></radialGradient>
      <radialGradient
        id="gradient-4"
        cx="50"
        cy="58"
        fx="50"
        fy="58"
        r="60"
        gradientTransform="translate(0 47) scale(1 0.2)"
        xlink:href="#gradient-3"
      ></radialGradient>
      <linearGradient
        id="gradient-5"
        x1="50"
        y1="90"
        x2="50"
        y2="10"
        gradientUnits="userSpaceOnUse"
      >
        <stop offset="0%" stop-color="black" stop-opacity="0.2"></stop>
        <stop offset="40%" stop-color="black" stop-opacity="0"></stop>
      </linearGradient>
    </defs>
    <g>
      <path
        d="M63,37c-6.7-4-4-27-13-27s-6.3,23-13,27-27,4-27,13,20.3,9,27,13,4,27,13,27,6.3-23,13-27,27-4,27-13-20.3-9-27-13Z"
        fill="currentColor"
      ></path>
      <path
        d="M63,37c-6.7-4-4-27-13-27s-6.3,23-13,27-27,4-27,13,20.3,9,27,13,4,27,13,27,6.3-23,13-27,27-4,27-13-20.3-9-27-13Z"
        fill="url(#gradient-1)"
      ></path>
      <path
        d="M63,37c-6.7-4-4-27-13-27s-6.3,23-13,27-27,4-27,13,20.3,9,27,13,4,27,13,27,6.3-23,13-27,27-4,27-13-20.3-9-27-13Z"
        fill="none"
        stroke="white"
        opacity="0.3"
        stroke-width="3"
        filter="url(#shine)"
        mask="url(#mask)"
      ></path>
      <path
        d="M63,37c-6.7-4-4-27-13-27s-6.3,23-13,27-27,4-27,13,20.3,9,27,13,4,27,13,27,6.3-23,13-27,27-4,27-13-20.3-9-27-13Z"
        fill="url(#gradient-2)"
      ></path>
      <path
        d="M63,37c-6.7-4-4-27-13-27s-6.3,23-13,27-27,4-27,13,20.3,9,27,13,4,27,13,27,6.3-23,13-27,27-4,27-13-20.3-9-27-13Z"
        fill="url(#gradient-3)"
      ></path>
      <path
        d="M63,37c-6.7-4-4-27-13-27s-6.3,23-13,27-27,4-27,13,20.3,9,27,13,4,27,13,27,6.3-23,13-27,27-4,27-13-20.3-9-27-13Z"
        fill="url(#gradient-4)"
      ></path>
      <path
        d="M63,37c-6.7-4-4-27-13-27s-6.3,23-13,27-27,4-27,13,20.3,9,27,13,4,27,13,27,6.3-23,13-27,27-4,27-13-20.3-9-27-13Z"
        fill="url(#gradient-5)"
      ></path>
    </g>
  </svg>
  <svg
    id="pegtopthree"
    width="100"
    height="100"
    xmlns="http://www.w3.org/2000/svg"
    xmlns:xlink="http://www.w3.org/1999/xlink"
    viewBox="0 0 100 100"
  >
    <defs>
      <filter id="shine">
        <feGaussianBlur stdDeviation="3"></feGaussianBlur>
      </filter>
      <mask id="mask">
        <path
          d="M63,37c-6.7-4-4-27-13-27s-6.3,23-13,27-27,4-27,13,20.3,9,27,13,4,27,13,27,6.3-23,13-27,27-4,27-13-20.3-9-27-13Z"
          fill="white"
        ></path>
      </mask>
      <radialGradient
        id="gradient-1"
        cx="50"
        cy="66"
        fx="50"
        fy="66"
        r="30"
        gradientTransform="translate(0 35) scale(1 0.5)"
        gradientUnits="userSpaceOnUse"
      >
        <stop offset="0%" stop-color="black" stop-opacity="0.3"></stop>
        <stop offset="50%" stop-color="black" stop-opacity="0.1"></stop>
        <stop offset="100%" stop-color="black" stop-opacity="0"></stop>
      </radialGradient>
      <radialGradient
        id="gradient-2"
        cx="55"
        cy="20"
        fx="55"
        fy="20"
        r="30"
        gradientUnits="userSpaceOnUse"
      >
        <stop offset="0%" stop-color="white" stop-opacity="0.3"></stop>
        <stop offset="50%" stop-color="white" stop-opacity="0.1"></stop>
        <stop offset="100%" stop-color="white" stop-opacity="0"></stop>
      </radialGradient>
      <radialGradient
        id="gradient-3"
        cx="85"
        cy="50"
        fx="85"
        fy="50"
        xlink:href="#gradient-2"
      ></radialGradient>
      <radialGradient
        id="gradient-4"
        cx="50"
        cy="58"
        fx="50"
        fy="58"
        r="60"
        gradientTransform="translate(0 47) scale(1 0.2)"
        xlink:href="#gradient-3"
      ></radialGradient>
      <linearGradient
        id="gradient-5"
        x1="50"
        y1="90"
        x2="50"
        y2="10"
        gradientUnits="userSpaceOnUse"
      >
        <stop offset="0%" stop-color="black" stop-opacity="0.2"></stop>
        <stop offset="40%" stop-color="black" stop-opacity="0"></stop>
      </linearGradient>
    </defs>
    <g>
      <path
        d="M63,37c-6.7-4-4-27-13-27s-6.3,23-13,27-27,4-27,13,20.3,9,27,13,4,27,13,27,6.3-23,13-27,27-4,27-13-20.3-9-27-13Z"
        fill="currentColor"
      ></path>
      <path
        d="M63,37c-6.7-4-4-27-13-27s-6.3,23-13,27-27,4-27,13,20.3,9,27,13,4,27,13,27,6.3-23,13-27,27-4,27-13-20.3-9-27-13Z"
        fill="url(#gradient-1)"
      ></path>
      <path
        d="M63,37c-6.7-4-4-27-13-27s-6.3,23-13,27-27,4-27,13,20.3,9,27,13,4,27,13,27,6.3-23,13-27,27-4,27-13-20.3-9-27-13Z"
        fill="none"
        stroke="white"
        opacity="0.3"
        stroke-width="3"
        filter="url(#shine)"
        mask="url(#mask)"
      ></path>
      <path
        d="M63,37c-6.7-4-4-27-13-27s-6.3,23-13,27-27,4-27,13,20.3,9,27,13,4,27,13,27,6.3-23,13-27,27-4,27-13-20.3-9-27-13Z"
        fill="url(#gradient-2)"
      ></path>
      <path
        d="M63,37c-6.7-4-4-27-13-27s-6.3,23-13,27-27,4-27,13,20.3,9,27,13,4,27,13,27,6.3-23,13-27,27-4,27-13-20.3-9-27-13Z"
        fill="url(#gradient-3)"
      ></path>
      <path
        d="M63,37c-6.7-4-4-27-13-27s-6.3,23-13,27-27,4-27,13,20.3,9,27,13,4,27,13,27,6.3-23,13-27,27-4,27-13-20.3-9-27-13Z"
        fill="url(#gradient-4)"
      ></path>
      <path
        d="M63,37c-6.7-4-4-27-13-27s-6.3,23-13,27-27,4-27,13,20.3,9,27,13,4,27,13,27,6.3-23,13-27,27-4,27-13-20.3-9-27-13Z"
        fill="url(#gradient-5)"
      ></path>
    </g>
  </svg>
</div>
</template>

<style scoped>
.loader {
  --fill-color: #ffffff;
  --shine-color: rgba(255, 255, 255, 0.2);
  color: var(--fill-color); /* Added to support currentColor fill */
  transform: scale(0.2); 
  width: 50px; 
  height: 20px;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  filter: drop-shadow(0 0 10px var(--shine-color));
  margin-top: 10px; 
}

/* Adjust absolute positioning to center relative to the container for the button context */
.loader #pegtopone {
  position: absolute;
  animation: flowe-one 1s linear infinite;
  /* Removed fill here as it is handled by the SVG elements */
}

/* ...rest of CSS animation ... */
.loader #pegtoptwo {
  position: absolute;
  opacity: 0;
  transform: scale(0) translateY(-200px) translateX(-100px);
  animation: flowe-two 1s linear infinite;
  animation-delay: 0.3s;
}

.loader #pegtopthree {
  position: absolute;
  opacity: 0;
  transform: scale(0) translateY(-200px) translateX(100px);
  animation: flowe-three 1s linear infinite;
  animation-delay: 0.6s;
}

/* Removed generic path rule as the new SVG has explicit fills */
/* .loader svg g path:first-child { ... } - REMOVED */

@keyframes flowe-one {
  0% {
    transform: scale(0.5) translateY(-200px);
    opacity: 0;
  }
  25% {
    transform: scale(0.75) translateY(-100px);
    opacity: 1;
  }
  50% {
    transform: scale(1) translateY(0px);
    opacity: 1;
  }
  75% {
    transform: scale(0.5) translateY(50px);
    opacity: 1;
  }
  100% {
    transform: scale(0) translateY(100px);
    opacity: 0;
  }
}

@keyframes flowe-two {
  0% {
    transform: scale(0.5) rotateZ(-10deg) translateY(-200px) translateX(-100px);
    opacity: 0;
  }
  25% {
    transform: scale(1) rotateZ(-5deg) translateY(-100px) translateX(-50px);
    opacity: 1;
  }
  50% {
    transform: scale(1) rotateZ(0deg) translateY(0px) translateX(-25px);
    opacity: 1;
  }
  75% {
    transform: scale(0.5) rotateZ(5deg) translateY(50px) translateX(0px);
    opacity: 1;
  }
  100% {
    transform: scale(0) rotateZ(10deg) translateY(100px) translateX(25px);
    opacity: 0;
  }
}

@keyframes flowe-three {
  0% {
    transform: scale(0.5) rotateZ(10deg) translateY(-200px) translateX(100px);
    opacity: 0;
  }
  25% {
    transform: scale(1) rotateZ(5deg) translateY(-100px) translateX(50px);
    opacity: 1;
  }
  50% {
    transform: scale(1) rotateZ(0deg) translateY(0px) translateX(25px);
    opacity: 1;
  }
  75% {
    transform: scale(0.5) rotateZ(-5deg) translateY(50px) translateX(0px);
    opacity: 1;
  }
  100% {
    transform: scale(0) rotateZ(-10deg) translateY(100px) translateX(-25px);
    opacity: 0;
  }
}
</style>
