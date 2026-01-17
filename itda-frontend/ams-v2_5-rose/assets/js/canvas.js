/**
 * AI Movie Studio v2.7 - Canvas Interactions
 * Node-based Scene Editor
 */

// ================================
// Canvas Controller
// ================================
class CanvasController {
    constructor(canvasElement) {
        this.canvas = canvasElement;
        this.scale = 1;
        this.offsetX = 0;
        this.offsetY = 0;
        this.isPanning = false;
        this.startX = 0;
        this.startY = 0;
        this.selectedNode = null;

        this.init();
    }

    init() {
        // Zoom with scroll
        this.canvas.addEventListener('wheel', (e) => {
            e.preventDefault();
            const delta = e.deltaY > 0 ? -0.1 : 0.1;
            this.zoom(delta, e.clientX, e.clientY);
        });

        // Pan with middle mouse or space+drag
        this.canvas.addEventListener('mousedown', (e) => {
            if (e.button === 1 || (e.button === 0 && e.target === this.canvas)) {
                this.startPan(e);
            }
        });

        document.addEventListener('mousemove', (e) => {
            if (this.isPanning) {
                this.pan(e);
            }
        });

        document.addEventListener('mouseup', () => {
            this.isPanning = false;
            this.canvas.style.cursor = 'grab';
        });

        // Node selection
        this.canvas.querySelectorAll('.node').forEach(node => {
            node.addEventListener('click', (e) => {
                e.stopPropagation();
                this.selectNode(node);
            });
        });

        // Deselect on canvas click
        this.canvas.addEventListener('click', () => {
            this.deselectAll();
        });

        // Keyboard shortcuts
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Delete' && this.selectedNode) {
                this.deleteNode(this.selectedNode);
            }
            if (e.key === 'Escape') {
                this.deselectAll();
            }
        });
    }

    zoom(delta, clientX, clientY) {
        const newScale = Math.max(0.25, Math.min(2, this.scale + delta));
        this.scale = newScale;
        this.updateTransform();
        this.updateZoomDisplay();
    }

    startPan(e) {
        this.isPanning = true;
        this.startX = e.clientX - this.offsetX;
        this.startY = e.clientY - this.offsetY;
        this.canvas.style.cursor = 'grabbing';
    }

    pan(e) {
        this.offsetX = e.clientX - this.startX;
        this.offsetY = e.clientY - this.startY;
        this.updateTransform();
    }

    updateTransform() {
        const container = this.canvas.querySelector('.nodes-container');
        if (container) {
            container.style.transform = `translate(-50%, 0) translate(${this.offsetX}px, ${this.offsetY}px) scale(${this.scale})`;
        }
    }

    updateZoomDisplay() {
        const display = document.getElementById('zoom-level');
        if (display) {
            display.textContent = Math.round(this.scale * 100) + '%';
        }
    }

    selectNode(node) {
        this.deselectAll();
        node.classList.add('selected');
        this.selectedNode = node;
        this.showNodeDetails(node);
    }

    deselectAll() {
        this.canvas.querySelectorAll('.node.selected').forEach(n => {
            n.classList.remove('selected');
        });
        this.selectedNode = null;
    }

    showNodeDetails(node) {
        const sidebar = document.getElementById('node-sidebar');
        const nodeType = node.dataset.nodeType;
        const nodeId = node.dataset.nodeId;

        // Update sidebar content based on node type
        if (sidebar) {
            sidebar.querySelector('.node-detail-type').textContent = nodeType;
            sidebar.querySelector('.node-detail-id').textContent = nodeId;
        }
    }

    deleteNode(node) {
        if (confirm('이 노드를 삭제하면 하위 노드들도 함께 삭제됩니다. 계속하시겠습니까?')) {
            node.remove();
            this.selectedNode = null;
            Toast.info('노드 삭제됨', '노드가 삭제되었습니다.');
        }
    }

    zoomIn() {
        this.zoom(0.1, window.innerWidth / 2, window.innerHeight / 2);
    }

    zoomOut() {
        this.zoom(-0.1, window.innerWidth / 2, window.innerHeight / 2);
    }

    fitToScreen() {
        this.scale = 1;
        this.offsetX = 0;
        this.offsetY = 0;
        this.updateTransform();
        this.updateZoomDisplay();
    }
}

// ================================
// Node Status Manager
// ================================
const NodeStatus = {
    _etaTimers: new Map(),

    setState(node, state) {
        // Remove all state classes
        node.classList.remove('state-pending', 'state-running', 'state-done', 'state-failed');
        node.classList.add(`state-${state}`);

        // Update badge
        const badge = node.querySelector('.node-status');
        if (badge) {
            badge.className = `node-status node-status-${state}`;

            const icons = {
                pending: 'clock',
                running: 'loader-2',
                done: 'check-circle',
                failed: 'x-circle'
            };

            const labels = {
                pending: '대기',
                running: '생성 중',
                done: '완료',
                failed: '실패'
            };

            badge.innerHTML = `
                <i data-lucide="${icons[state]}" class="w-3 h-3 ${state === 'running' ? 'animate-spin' : ''}"></i>
                ${labels[state]}
            `;
            if (window.lucide) lucide.createIcons();
        }

        // Toggle progress block (if present)
        const progress = node.querySelector('[data-node-progress]');
        if (progress) {
            progress.classList.toggle('hidden', state !== 'running');

            const fill = progress.querySelector('.node-progress-fill');
            if (fill && state !== 'running') {
                fill.style.transition = '';
                fill.style.width = '0%';
            }
        }

        // Stop ETA timer when leaving running state
        const timer = this._etaTimers.get(node);
        if (timer && state !== 'running') {
            clearInterval(timer);
            this._etaTimers.delete(node);
        }
    },

    startProgress(node, duration = 10000, etaSeconds = null) {
        const progressBar = node.querySelector('.node-progress-fill');
        if (progressBar) {
            progressBar.style.width = '0%';
            progressBar.style.transition = `width ${duration}ms linear`;

            requestAnimationFrame(() => {
                progressBar.style.width = '100%';
            });
        }

        const etaEl = node.querySelector('[data-node-eta]');
        if (etaEl) {
            const start = Date.now();
            const totalMs = duration;
            const totalSeconds = typeof etaSeconds === 'number' ? Math.max(0, Math.round(etaSeconds)) : Math.round(totalMs / 1000);
            const initial = totalSeconds;
            etaEl.textContent = `~${initial}s`;

            const tick = () => {
                const elapsed = Date.now() - start;
                const remaining = typeof etaSeconds === 'number'
                    ? Math.max(0, totalSeconds - Math.round(elapsed / 1000))
                    : Math.max(0, Math.round((totalMs - elapsed) / 1000));
                etaEl.textContent = `~${remaining}s`;
                if (remaining <= 0) {
                    clearInterval(interval);
                    this._etaTimers.delete(node);
                }
            };

            const interval = setInterval(tick, 1000);
            this._etaTimers.set(node, interval);
        }
    },

    completeProgress(node, success = true) {
        this.setState(node, success ? 'done' : 'failed');

        if (success) {
            Toast.show({
                type: 'success',
                title: '생성 완료',
                message: `${node.dataset.nodeType} 노드가 생성되었습니다.`,
                actions: [
                    {
                        id: 'open',
                        label: '열기',
                        onClick: () => {
                            node.scrollIntoView({ behavior: 'smooth', block: 'center' });
                            window.canvasController?.selectNode(node);
                        }
                    }
                ]
            });
        } else {
            Toast.error('생성 실패', 'AI 생성 중 오류가 발생했습니다.', [
                { id: 'retry', label: '재시도', onClick: () => this.retry(node) },
                {
                    id: 'edit',
                    label: '프롬프트 수정',
                    secondary: true,
                    onClick: () => document.querySelector('#node-sidebar textarea')?.focus()
                },
                {
                    id: 'back',
                    label: '이전 단계로',
                    secondary: true,
                    onClick: () => {
                        const prev = node.previousElementSibling;
                        if (prev && prev.classList.contains('node')) {
                            prev.scrollIntoView({ behavior: 'smooth', block: 'center' });
                            window.canvasController?.selectNode(prev);
                        }
                    }
                }
            ]);
        }
    },

    retry(node) {
        this.setState(node, 'running');
        this.startProgress(node, 5000);

        setTimeout(() => {
            this.completeProgress(node, true);
        }, 5000);
    }
};

// ================================
// Mini Timeline
// ================================
const MiniTimeline = {
    clips: [],
    storageKey: 'ams.timeline.clips:v1',

    add(clip) {
        this.clips.push(clip);
        this.render();
        Toast.success('타임라인에 추가', `영상이 타임라인에 확정되었습니다.`);
    },

    remove(clipId) {
        this.clips = this.clips.filter(c => c.id !== clipId);
        this.render();
    },

    render() {
        const container = document.getElementById('mini-timeline-clips');
        if (!container) return;

        let totalDuration = 0;
        container.innerHTML = this.clips.map(clip => {
            totalDuration += clip.duration;
            return `
                <div class="mini-timeline-clip" data-clip-id="${clip.id}">
                    <img src="${clip.thumbnail}" alt="">
                    <span>${clip.duration}s</span>
                </div>
            `;
        }).join('');

        const durationDisplay = document.getElementById('timeline-duration');
        if (durationDisplay) {
            durationDisplay.textContent = `총 ${totalDuration}초 / 60초`;
        }

        try {
            const stored = this.clips.map(c => ({
                id: c.id,
                thumbnail: c.thumbnail,
                duration: c.duration,
                label: c.label || ''
            }));
            window.AMS?.storage?.setJSON?.(this.storageKey, stored) ?? localStorage.setItem(this.storageKey, JSON.stringify(stored));
        } catch {
            // ignore
        }
    },

    load() {
        try {
            const stored = window.AMS?.storage?.getJSON?.(this.storageKey, null) ?? JSON.parse(localStorage.getItem(this.storageKey) || 'null');
            if (!Array.isArray(stored)) return;
            this.clips = stored.filter(Boolean);
            this.render();
        } catch {
            // ignore
        }
    }
};

// ================================
// Initialize on DOM Ready
// ================================
document.addEventListener('DOMContentLoaded', () => {
    const canvas = document.querySelector('.canvas-bg');
    if (canvas) {
        window.canvasController = new CanvasController(canvas);
    }

    // Zoom controls
    document.getElementById('zoom-in')?.addEventListener('click', () => {
        window.canvasController?.zoomIn();
    });

    document.getElementById('zoom-out')?.addEventListener('click', () => {
        window.canvasController?.zoomOut();
    });

    document.getElementById('zoom-fit')?.addEventListener('click', () => {
        window.canvasController?.fitToScreen();
    });

    // Restore mini timeline from storage (if present)
    MiniTimeline.load();
});

// Export for global use
window.NodeStatus = NodeStatus;
window.MiniTimeline = MiniTimeline;
