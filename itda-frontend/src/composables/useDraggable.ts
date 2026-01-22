/**
 * useDraggable - 엘리먼트를 드래그 가능하게 만드는 composable
 * 
 * 사용법:
 * const { position, isDragging, onMouseDown, shouldPreventClick } = useDraggable({ initialX, initialY, storageKey });
 * <div :style="{ right: position.right + 'px', bottom: position.bottom + 'px' }" @mousedown="onMouseDown">...</div>
 */
import { ref, reactive, onMounted, onUnmounted } from 'vue';

export interface DraggableOptions {
    /** 초기 X 위치 (right로부터의 거리, px) */
    initialRight?: number;
    /** 초기 Y 위치 (bottom으로부터의 거리, px) */
    initialBottom?: number;
    /** localStorage 저장 키 (위치 기억용) */
    storageKey?: string;
    /** 드래그 영역 제한 여부 */
    constrainToWindow?: boolean;
}

export interface DraggablePosition {
    right: number;
    bottom: number;
}

export function useDraggable(options: DraggableOptions = {}) {
    const {
        initialRight = 24,
        initialBottom = 24,
        storageKey,
        constrainToWindow = true,
    } = options;

    // 위치 상태
    const position = reactive<DraggablePosition>({
        right: initialRight,
        bottom: initialBottom,
    });

    // 드래그 상태
    const isDragging = ref(false);
    const hasMoved = ref(false); // 실제로 이동했는지 여부
    const dragOffset = reactive({ x: 0, y: 0 });

    // localStorage에서 위치 복원
    function loadPosition(): void {
        if (!storageKey) return;
        const saved = localStorage.getItem(storageKey);
        if (saved) {
            try {
                const parsed = JSON.parse(saved);
                position.right = parsed.right ?? initialRight;
                position.bottom = parsed.bottom ?? initialBottom;
            } catch {
                // 파싱 실패 시 기본값 사용
            }
        }
    }

    // localStorage에 위치 저장
    function savePosition(): void {
        if (!storageKey) return;
        localStorage.setItem(storageKey, JSON.stringify({
            right: position.right,
            bottom: position.bottom,
        }));
    }

    // 마우스 다운 핸들러 - 드래그 시작
    function onMouseDown(event: MouseEvent): void {
        // 버튼 내부 요소 클릭 시에도 드래그 시작
        isDragging.value = true;
        hasMoved.value = false;

        // 현재 마우스 위치와 엘리먼트 위치의 차이 계산
        const windowWidth = window.innerWidth;
        const windowHeight = window.innerHeight;

        dragOffset.x = windowWidth - event.clientX - position.right;
        dragOffset.y = windowHeight - event.clientY - position.bottom;

        // 텍스트 선택 방지
        event.preventDefault();
    }

    // 마우스 이동 핸들러
    function onMouseMove(event: MouseEvent): void {
        if (!isDragging.value) return;

        hasMoved.value = true; // 실제로 마우스가 이동함

        const windowWidth = window.innerWidth;
        const windowHeight = window.innerHeight;

        let newRight = windowWidth - event.clientX - dragOffset.x;
        let newBottom = windowHeight - event.clientY - dragOffset.y;

        // 화면 경계 제한
        if (constrainToWindow) {
            const minMargin = 10;
            const maxRight = windowWidth - 100; // 최소 100px 너비는 보이도록
            const maxBottom = windowHeight - 50; // 최소 50px 높이는 보이도록

            newRight = Math.max(minMargin, Math.min(maxRight, newRight));
            newBottom = Math.max(minMargin, Math.min(maxBottom, newBottom));
        }

        position.right = newRight;
        position.bottom = newBottom;
    }

    // 마우스 업 핸들러 - 드래그 종료
    function onMouseUp(): void {
        if (isDragging.value) {
            isDragging.value = false;
            if (hasMoved.value) {
                savePosition();
            }
        }
    }

    // 클릭 방지 체크 (드래그 후 클릭 방지용)
    function shouldPreventClick(): boolean {
        return hasMoved.value;
    }

    // 이벤트 리스너 등록/해제
    onMounted(() => {
        loadPosition();
        document.addEventListener('mousemove', onMouseMove);
        document.addEventListener('mouseup', onMouseUp);
    });

    onUnmounted(() => {
        document.removeEventListener('mousemove', onMouseMove);
        document.removeEventListener('mouseup', onMouseUp);
    });

    return {
        position,
        isDragging,
        onMouseDown,
        shouldPreventClick,
        resetPosition: () => {
            position.right = initialRight;
            position.bottom = initialBottom;
            savePosition();
        },
    };
}
