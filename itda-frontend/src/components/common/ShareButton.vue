<script setup lang="ts">
import { Share2 } from 'lucide-vue-next'

const emit = defineEmits<{
  (e: 'click'): void
}>()
</script>

<template>
  <button class="button-content" @click="emit('click')">
    <span class="text">공유</span>
    <Share2 class="share-icon" />
  </button>
</template>

<style scoped>
/* Button Styles */
.button-content {
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--rose-400), var(--rose-600));
  color: white;
  padding: 0.6rem 1.2rem; /* Adjusted padding for header context */
  border-radius: 50px;
  cursor: pointer;
  border: none;
  font-family: inherit;
  transition:
    background 0.4s cubic-bezier(0.25, 0.8, 0.25, 1),
    transform 0.3s ease,
    box-shadow 0.4s ease;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1);
  position: relative;
  z-index: 10;
  overflow: hidden;
  height: 40px; /* Fixed height for consistency */
}

.button-content::before {
  content: "";
  position: absolute;
  inset: 0;
  border-radius: inherit;
  background: linear-gradient(
    135deg,
    rgba(251, 113, 133, 0.4),
    rgba(225, 29, 72, 0.4)
  );
  filter: blur(15px);
  opacity: 0;
  transition: opacity 0.5s ease;
  z-index: -1;
}

.button-content::after {
  content: "";
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(
    circle,
    rgba(255, 255, 255, 0.3) 0%,
    rgba(255, 255, 255, 0) 70%
  );
  transform: scale(0);
  transition: transform 0.6s ease-out;
  z-index: -1;
}

.button-content:hover::before {
  opacity: 1;
}

.button-content:hover::after {
  transform: scale(1);
}

.button-content:hover {
  background: linear-gradient(135deg, var(--rose-500), var(--rose-700));
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.15);
  transform: translateY(-2px) scale(1.02);
}

.button-content:active {
  transform: translateY(0) scale(0.98);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

.text {
  font-size: 0.95rem;
  font-weight: 600;
  margin-right: 8px;
  white-space: nowrap;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
  transition: letter-spacing 0.3s ease;
}

.button-content:hover .text {
  letter-spacing: 0.5px;
}

.share-icon {
  width: 18px;
  height: 18px;
  /* Lucide icons use stroke, user CSS used fill. 
     We keep stroke white. */
  stroke: white;
  stroke-width: 2.5;
  transition:
    transform 0.4s cubic-bezier(0.68, -0.55, 0.265, 1.55),
    stroke 0.3s ease;
  filter: drop-shadow(0 1px 2px rgba(0, 0, 0, 0.1));
}

.button-content:hover .share-icon {
  transform: rotate(180deg) scale(1.1);
}

/* Reduced Motion */
@media (prefers-reduced-motion: reduce) {
  .button-content,
  .share-icon {
    transition: none;
  }
}
</style>
