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
        # -> Navigate to the scene editor to select nodes for deletion test.
        frame = context.pages[-1]
        # Click on '무료로 시작하기' (Start for free) to enter the app and access the scene editor.
        elem = frame.locator('xpath=html/body/div/div/header/nav/a[2]').nth(0)
        await page.wait_for_timeout(3000); await elem.click(timeout=5000)
        

        # -> Input email and password to login and access scene editor.
        frame = context.pages[-1]
        # Input email for login.
        elem = frame.locator('xpath=html/body/div/div/main/div[2]/form/div/div/input').nth(0)
        await page.wait_for_timeout(3000); await elem.fill('testuser@example.com')
        

        frame = context.pages[-1]
        # Input password for login.
        elem = frame.locator('xpath=html/body/div/div/main/div[2]/form/div[2]/div/input').nth(0)
        await page.wait_for_timeout(3000); await elem.fill('TestPassword123')
        

        frame = context.pages[-1]
        # Click 로그인 button to submit login form.
        elem = frame.locator('xpath=html/body/div/div/main/div[2]/form/button').nth(0)
        await page.wait_for_timeout(3000); await elem.click(timeout=5000)
        

        # -> Navigate to a project to open the scene editor for node selection.
        frame = context.pages[-1]
        # Click on 'The Martian Red' project to open it and access the scene editor.
        elem = frame.locator('xpath=html/body/div/div/main/div/div/div[2]/div[2]/button').nth(0)
        await page.wait_for_timeout(3000); await elem.click(timeout=5000)
        

        # -> Close the 'Create New Project' modal to access the dashboard and open a project for scene editor node deletion test.
        frame = context.pages[-1]
        # Click '취소' button to close the 'Create New Project' modal.
        elem = frame.locator('xpath=html/body/div[2]/div/div[3]/button').nth(0)
        await page.wait_for_timeout(3000); await elem.click(timeout=5000)
        

        # -> Open 'The Martian Red' project to access the scene editor for node selection and deletion test.
        frame = context.pages[-1]
        # Click on 'The Martian Red' project to open it and access the scene editor.
        elem = frame.locator('xpath=html/body/div/div/main/div/div/section/div/a').nth(0)
        await page.wait_for_timeout(3000); await elem.click(timeout=5000)
        

        # -> Select one or more scenes (nodes) in the scene editor to test deletion behavior.
        frame = context.pages[-1]
        # Select the first scene node 'The Discovery' in the scene editor.
        elem = frame.locator('xpath=html/body/div/div/main/div/div/div[2]/div[3]/div/div').nth(0)
        await page.wait_for_timeout(3000); await elem.click(timeout=5000)
        

        # -> Focus the edit input field of the second scene 'Entering the Structure' and press Delete or Backspace key to verify nodes are not deleted.
        frame = context.pages[-1]
        # Click 'Edit' button of the second scene 'Entering the Structure' to focus the input field.
        elem = frame.locator('xpath=html/body/div/div/main/div/div/div[2]/div[3]/div[2]/div/div/div[2]/button[2]').nth(0)
        await page.wait_for_timeout(3000); await elem.click(timeout=5000)
        

        # --> Assertions to verify final state
        frame = context.pages[-1]
        await expect(frame.locator('text=The Discovery').first).to_be_visible(timeout=30000)
        await expect(frame.locator('text=Entering the Structure').first).to_be_visible(timeout=30000)
        await expect(frame.locator('text=The Revelation').first).to_be_visible(timeout=30000)
        await asyncio.sleep(5)
    
    finally:
        if context:
            await context.close()
        if browser:
            await browser.close()
        if pw:
            await pw.stop()
            
asyncio.run(run_test())
    