# Flyff U Commander

A powerful and enhanced WebViewClient for playing Flyff Universe on Android. It allows you to play with multiple clients, automate tasks with macros, and access useful utilities.

## Origin and License

This project is a modified version of the [FlyffU WebView Client](https://github.com/ils94/FlyffUAndroid). It is released under the **GNU General Public License v3.0 (GPLv3)**.

**Modifications:** This version includes significant enhancements, such as advanced action button functionalities (macros, timed repeats), improved persistence, and a dedicated hide/show button for action buttons.

## Features

[Demo Video](https://www.youtube.com/watch?v=Gwpo5B9kRac)

*   **Immersive Fullscreen**: Enjoy a truly immersive experience with the application running in full screen.
*   **Movable Floating Action Button (FAB)**: The main FAB can be dragged and repositioned anywhere on the screen for convenience.
*   **Multiple Clients**: Open and manage several game instances simultaneously.
*   **Quick Switching**: Easily switch between open clients with a tap on the floating action button (FAB).
*   **Rename Clients**: Customize the names of your clients for better organization.
*   **Delete Clients**: Remove clients and their saved data when you no longer need them.
*   **Utilities Menu**: Quickly access useful websites such as:
    *   Flyffipedia (formerly Flyff Wiki)
    *   Madrigal Inside
    *   Flyffulator
    *   Flyff Skill Simulator
*   **Utility Page Controls**: For utility pages (Flyffipedia, Madrigal Inside, Flyffulator, Flyff Skill Simulator), three fixed FABs appear in the top right: Reload Page, Go Back, and Close WebView.
*   **Data Persistence**: Each client's data (including action button configurations, positions, colors, and macro states) is saved individually, allowing you to continue where you left off.
*   **Action Buttons (ABs)**: Create custom on-screen buttons for various actions.
    *   **Normal Buttons**: Dispatch a single key press (e.g., 'F1', '1', 'A').
    *   **Custom Key Buttons**: Assigns a single custom character key. Can optionally include 'Alt' or 'Ctrl' modifiers. When a modifier is active, the key must be a digit from 0-9.
    *   **Macro Buttons**: Execute a sequence of key presses with a customizable delay between each key (e.g., "1,2,3"). Can optionally include 'Alt' or 'Ctrl' modifiers. When a modifier is active, keys must be digits from 0-9.
    *   **Timed Repeat Macro Buttons**: Toggle continuous repetition of a single key press at a user-defined interval. The button's color visually indicates its active/inactive state. Can optionally include 'Alt' or 'Ctrl' modifiers. When a modifier is active, the key must be a digit from 0-9.
    *   **Combo Buttons**: Sends a sequence of three key presses: a selected Function Key (F1-F12), followed by a digit key (0-9), and then another selected Function Key. (e.g., F2 + 9 + F1).
*   **Hide/Show Action Buttons**: A dedicated button will appear on the screen if any action buttons are created. Tap this button to toggle the visibility of all action buttons. Its state (hidden/shown) is automatically saved across app restarts.
*   **Fixed Action Button Positions**: Long-press the Hide/Show Action Buttons FAB to toggle whether action button positions are fixed or draggable. This setting is persistent across app restarts.

## How to Use

1.  **Installation**: Download and install the latest APK.
2.  **Immersive Fullscreen**: The application automatically enters immersive fullscreen mode upon launch.
3.  **Movable FABs**:
    *   The main FAB can be dragged and repositioned anywhere on the screen.
    *   Action buttons can be dragged and repositioned unless their positions are fixed (see point 7).
    *   Their positions are automatically saved.
4.  **Open/Create Clients**:
    *   Upon launch, a default client will be opened.
    *   To create a new client, long-press the main Floating Action Button (FAB).
    *   In the menu that appears, select "New Client".
5.  **Switch Between Clients**:
    *   Tap the main FAB to switch to the next open client.
6.  **Manage Clients (Long-press the main FAB)**:
    *   **Clients**:
        *   **Switch to**: Switch to a specific client.
        *   **Kill**: Close an open client (data is retained).
        *   **Open**: Open a saved client that is not currently active.
        *   **Rename**: Change a client's name.
        *   **Delete**: Permanently remove a client and all its data.
    *   **Action Buttons**:
        *   **New**: Create a new action button. You can choose from:
            *   **Function Key**:
                *   **Single Button (change active bar)**: Assigns a standard function key (F1-F12).
                *   **Combo Button**: Select a main Function Key (F1-F12), a digit key (0-9), and an "After pressed go to bar" Function Key (F1-F12). The button will send these keys sequentially (e.g., F2, then 9, then F1).
            *   **Custom Key**: Assigns a single custom character key. You can select 'Alt' or 'Ctrl' checkboxes to send the key as a combo (e.g., Alt+1). If a modifier is selected, the key input is restricted to digits 0-9.
            *   **Macro**: Defines a sequence of keys (e.g., "1,2,3") with a customizable name (max 2 letters) and a delay between 0.5s and 5s. You can select 'Alt' or 'Ctrl' checkboxes to send each key in the macro as a combo (e.g., Alt+1, Alt+2). If a modifier is selected, key inputs are restricted to digits 0-9.
            *   **Timed Repeat Macro**: Sets a single key to repeat at a specified interval, toggled on/off. Customizable name (max 2 letters) and repeat interval between 0.5s and 20s. You can select 'Alt' or 'Ctrl' checkboxes to send the repeating key as a combo (e.g., Ctrl+5). If a modifier is selected, the key input is restricted to digits 0-9.
        *   **Color**: Change the color of an existing action button or all buttons for the selected client.
        *   **Delete**: Remove an existing action button.
    *   **Utils**:
        *   **Flyffipedia**: Opens the Flyffipedia website.
        *   **Madrigal Inside**: Opens the Madrigal Inside website.
        *   **Flyffulator**: Opens the Flyffulator.
        *   **Flyff Skill Simulator**: Opens the Flyff Skill Simulator.
        *   For utility pages, three fixed buttons will appear in the top right: Reload Page, Go Back, and Close WebView.
7.  **Hide/Show Action Buttons**:
    *   A dedicated hide/show button will appear on the screen if any action buttons are created.
    *   Tap this button to toggle the visibility of all action buttons.
    *   **Long-press** this button to toggle whether action button positions are fixed (not draggable) or draggable.
    *   Both the visibility state and the fixed/draggable state are automatically saved across app restarts.
8.  **Exit Application**: Press your device's "Back" button twice to exit.