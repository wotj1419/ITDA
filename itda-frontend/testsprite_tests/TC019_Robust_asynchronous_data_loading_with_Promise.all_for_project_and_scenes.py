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
        # -> Click 로그인 (login) to authenticate and access project detail page for testing asynchronous data loading.
        frame = context.pages[-1]
        # Click 로그인 to go to login page
        elem = frame.locator('xpath=html/body/div/div/header/nav/a').nth(0)
        await page.wait_for_timeout(3000); await elem.click(timeout=5000)
        

        # -> Input valid email and password, then submit login form to authenticate.
        frame = context.pages[-1]
        # Input valid email for login
        elem = frame.locator('xpath=html/body/div/div/main/div[2]/form/div/div/input').nth(0)
        await page.wait_for_timeout(3000); await elem.fill('testuser@example.com')
        

        frame = context.pages[-1]
        # Input valid password for login
        elem = frame.locator('xpath=html/body/div/div/main/div[2]/form/div[2]/div/input').nth(0)
        await page.wait_for_timeout(3000); await elem.fill('TestPassword123')
        

        frame = context.pages[-1]
        # Click 로그인 button to submit login form
        elem = frame.locator('xpath=html/body/div/div/main/div[2]/form/button').nth(0)
        await page.wait_for_timeout(3000); await elem.click(timeout=5000)
        

        # -> Click on a project (e.g., The Martian Red) to load its detail page and trigger asynchronous loading of project and scenes data.
        frame = context.pages[-1]
        # Click on The Martian Red project to load project detail page
        elem = frame.locator('xpath=html/body/div/div/main/div/div/section/div/a').nth(0)
        await page.wait_for_timeout(3000); await elem.click(timeout=5000)
        

        # -> Reload the project detail page to restore UI and verify asynchronous data loading and UI updates again.
        await page.goto('http://localhost:5173/projects/1', timeout=10000)
        await asyncio.sleep(3)
        

        # --> Assertions to verify final state
        frame = context.pages[-1]
        await expect(frame.locator('text=The Martian Red').first).to_be_visible(timeout=30000)
        await expect(frame.locator('text=Sci-Fi').first).to_be_visible(timeout=30000)
        await expect(frame.locator('text=1/3').first).to_be_visible(timeout=30000)
        await expect(frame.locator('text=The Discovery').first).to_be_visible(timeout=30000)
        await expect(frame.locator('text=Mark walks through the red desert and spots a metallic glint in the distance.').first).to_be_visible(timeout=30000)
        await expect(frame.locator('text=Entering the Structure').first).to_be_visible(timeout=30000)
        await expect(frame.locator('text=Inside the dark corridor, bioluminescent plants light up the path.').first).to_be_visible(timeout=30000)
        await expect(frame.locator('text=The Revelation').first).to_be_visible(timeout=30000)
        await expect(frame.locator('text=Mark discovers ancient alien technology that could save Earth.').first).to_be_visible(timeout=30000)
        await asyncio.sleep(5)
    
    finally:
        if context:
            await context.close()
        if browser:
            await browser.close()
        if pw:
            await pw.stop()
            
asyncio.run(run_test())
    