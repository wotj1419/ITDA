import asyncio
from playwright import async_api
from playwright.async_api import expect

async def run_test():
    pw = None
    browser = None
    context = None
    
    try:
        # Start a Playwright session in asynchronous mode
        pw = await async_api.async_playwright().start()
        
        # Launch a Chromium browser in headless mode with custom arguments
        browser = await pw.chromium.launch(
            headless=True,
            args=[
                "--window-size=1280,720",         # Set the browser window size
                "--disable-dev-shm-usage",        # Avoid using /dev/shm which can cause issues in containers
                "--ipc=host",                     # Use host-level IPC for better stability
                "--single-process"                # Run the browser in a single process mode
            ],
        )
        
        # Create a new browser context (like an incognito window)
        context = await browser.new_context()
        context.set_default_timeout(5000)
        
        # Open a new page in the browser context
        page = await context.new_page()
        
        # Navigate to your target URL and wait until the network request is committed
        await page.goto("http://localhost:5173", wait_until="commit", timeout=10000)
        
        # Wait for the main page to reach DOMContentLoaded state (optional for stability)
        try:
            await page.wait_for_load_state("domcontentloaded", timeout=3000)
        except async_api.Error:
            pass
        
        # Iterate through all iframes and wait for them to load as well
        for frame in page.frames:
            try:
                await frame.wait_for_load_state("domcontentloaded", timeout=3000)
            except async_api.Error:
                pass
        
        # Interact with the page elements to simulate user flow
        # -> Manually construct and load a URL with valid projectId and sceneId parameters to verify correct parsing and data display.
        await page.goto('http://localhost:5173/project?projectId=123&sceneId=456', timeout=10000)
        await asyncio.sleep(3)
        

        # -> Try to find and click a navigation element or button that leads to the project or scene page to verify if the parameters are used there.
        await page.mouse.wheel(0, 500)
        

        frame = context.pages[-1]
        # Click the '무료로 시작하기' (Start for free) button to see if it navigates to project or scene page using URL parameters
        elem = frame.locator('xpath=html/body/div/div/section/div[4]/a').nth(0)
        await page.wait_for_timeout(3000); await elem.click(timeout=5000)
        

        # -> Input valid login credentials and submit to proceed to project/scene pages.
        frame = context.pages[-1]
        # Input email for login
        elem = frame.locator('xpath=html/body/div/div/main/div[2]/form/div/div/input').nth(0)
        await page.wait_for_timeout(3000); await elem.fill('testuser@example.com')
        

        frame = context.pages[-1]
        # Input password for login
        elem = frame.locator('xpath=html/body/div/div/main/div[2]/form/div[2]/div/input').nth(0)
        await page.wait_for_timeout(3000); await elem.fill('TestPassword123')
        

        frame = context.pages[-1]
        # Click login button to submit credentials
        elem = frame.locator('xpath=html/body/div/div/main/div[2]/form/button').nth(0)
        await page.wait_for_timeout(3000); await elem.click(timeout=5000)
        

        # -> Manually construct and load a URL with a valid projectId parameter to verify the project page loads the correct project data.
        await page.goto('http://localhost:5173/project?projectId=14', timeout=10000)
        await asyncio.sleep(3)
        

        # -> Check if user is logged out or if there is a need to log in again to access project pages. If logged out, perform login and then retry loading project page with projectId parameter.
        frame = context.pages[-1]
        # Click 로그인 (Login) link to check login status or log in again
        elem = frame.locator('xpath=html/body/div/div/header/nav/a').nth(0)
        await page.wait_for_timeout(3000); await elem.click(timeout=5000)
        

        # -> Input valid login credentials and submit to log in and then test project and scene URL parameters.
        frame = context.pages[-1]
        # Input email for login
        elem = frame.locator('xpath=html/body/div/div/main/div[2]/form/div/div/input').nth(0)
        await page.wait_for_timeout(3000); await elem.fill('testuser@example.com')
        

        frame = context.pages[-1]
        # Input password for login
        elem = frame.locator('xpath=html/body/div/div/main/div[2]/form/div[2]/div/input').nth(0)
        await page.wait_for_timeout(3000); await elem.fill('TestPassword123')
        

        frame = context.pages[-1]
        # Click login button to submit credentials
        elem = frame.locator('xpath=html/body/div/div/main/div[2]/form/button').nth(0)
        await page.wait_for_timeout(3000); await elem.click(timeout=5000)
        

        # -> Click on the project link 'The Martian Red' at index 13 to navigate to the project page and verify if the projectId parameter is parsed and used correctly.
        frame = context.pages[-1]
        # Click on 'The Martian Red' project link under All Projects
        elem = frame.locator('xpath=html/body/div/div/main/div/div/section[2]/div/a').nth(0)
        await page.wait_for_timeout(3000); await elem.click(timeout=5000)
        

        # -> Manually construct and load a URL with valid projectId and sceneId parameters to verify the scene page loads the correct scene data.
        await page.goto('http://localhost:5173/projects/1/scenes/2', timeout=10000)
        await asyncio.sleep(3)
        

        # -> Verify key features on the scene page such as Scene Editor nodes, AI generation prompts, Timeline confirm/playback, and Collaboration panel visibility as per PRD v2.5.
        frame = context.pages[-1]
        # Click the 'Scene Editor' button to verify scene editor nodes visibility and functionality
        elem = frame.locator('xpath=html/body/div/div/aside/nav/button').nth(0)
        await page.wait_for_timeout(3000); await elem.click(timeout=5000)
        

        frame = context.pages[-1]
        # Click the 'AI Movie Studio' link to verify AI generation prompts
        elem = frame.locator('xpath=html/body/div/div/main/header/div/nav/a').nth(0)
        await page.wait_for_timeout(3000); await elem.click(timeout=5000)
        

        frame = context.pages[-1]
        # Click the 'Scene Timeline' link to verify timeline confirm and playback features
        elem = frame.locator('xpath=html/body/div/div/main/header/div[2]/a').nth(0)
        await page.wait_for_timeout(3000); await elem.click(timeout=5000)
        

        # -> Click on the 'The Martian Red' project link at index 11 to navigate to the project page and verify if the projectId parameter is parsed and used correctly.
        frame = context.pages[-1]
        # Click on 'The Martian Red' project link in Quick Access
        elem = frame.locator('xpath=html/body/div/div/main/div/div/section/div/a').nth(0)
        await page.wait_for_timeout(3000); await elem.click(timeout=5000)
        

        # -> Manually construct and load a URL with valid projectId=1 and sceneId=2 parameters to verify the scene page loads the correct scene data and UI elements.
        await page.goto('http://localhost:5173/projects/1/scenes/2', timeout=10000)
        await asyncio.sleep(3)
        

        # -> Click on 'Scene Timeline' button to verify timeline confirm and playback features.
        frame = context.pages[-1]
        # Click the 'Scene Timeline' button to verify timeline confirm and playback features
        elem = frame.locator('xpath=html/body/div/div/main/header/div[2]/a').nth(0)
        await page.wait_for_timeout(3000); await elem.click(timeout=5000)
        

        # -> Click the '채팅' (Chat) button to verify collaboration panel visibility.
        frame = context.pages[-1]
        # Click the '채팅' (Chat) button to verify collaboration panel visibility
        elem = frame.locator('xpath=html/body/div/div[4]/div/button[4]').nth(0)
        await page.wait_for_timeout(3000); await elem.click(timeout=5000)
        

        # --> Assertions to verify final state
        frame = context.pages[-1]
        await expect(frame.locator('text=The Martian Red').first).to_be_visible(timeout=30000)
        await expect(frame.locator('text=Full Timeline').first).to_be_visible(timeout=30000)
        await expect(frame.locator('text=0').first).to_be_visible(timeout=30000)
        await expect(frame.locator('text=클립').first).to_be_visible(timeout=30000)
        await expect(frame.locator('text=총 길이').first).to_be_visible(timeout=30000)
        await expect(frame.locator('text=AI Movie Studio').first).to_be_visible(timeout=30000)
        await expect(frame.locator('text=영상 병합하기').first).to_be_visible(timeout=30000)
        await expect(frame.locator('text=미리보기').first).to_be_visible(timeout=30000)
        await expect(frame.locator('text=재생').first).to_be_visible(timeout=30000)
        await expect(frame.locator('text=00:00 / 00:00').first).to_be_visible(timeout=30000)
        await expect(frame.locator('text=메인 비디오 트랙 (확정 클립)').first).to_be_visible(timeout=30000)
        await expect(frame.locator('text=드래그하여 순서 변경').first).to_be_visible(timeout=30000)
        await expect(frame.locator('text=확정된 클립이 없습니다').first).to_be_visible(timeout=30000)
        await expect(frame.locator('text=0개 클립').first).to_be_visible(timeout=30000)
        await expect(frame.locator('text=총 길이: 0초').first).to_be_visible(timeout=30000)
        await expect(frame.locator('text=Live').first).to_be_visible(timeout=30000)
        await asyncio.sleep(5)
    
    finally:
        if context:
            await context.close()
        if browser:
            await browser.close()
        if pw:
            await pw.stop()
            
asyncio.run(run_test())
    