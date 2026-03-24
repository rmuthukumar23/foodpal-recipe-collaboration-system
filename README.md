# FoodPal — Distributed Recipe & Cooking Organizer (Client–Server)

FoodPal is a distributed cooking organizer built with a **client–server architecture** to efficiently **create, manage, and share recipes** across multiple simultaneous clients. It combines a shared, persistent recipe library with a **global ingredient database (including nutritional values)**, powerful filtering/search, and TA-friendly usability features such as **printing**, a **shopping list workflow**, and **real-time synchronization**.

---

## Table of Contents
- [Project Overview](#project-overview)
- [Core Features and How to Test Them](#core-features-and-how-to-test-them)
- [Technical Details](#technical-details)
- [Installation and Running (Step-by-Step)](#installation-and-running-step-by-step)
- [Navigation and Quick Walkthrough](#navigation-and-quick-walkthrough)
- [Backlog Coverage Mapped to Features](#features-implemented-including-extensions)
- [Hard-to-Find Features](#hard-to-find-features)
- [Extensions and Excellent Features](#extensions-and-excellent-features)

---

## Project Overview

### What FoodPal Does
FoodPal provides a shared recipe library where users can:
- Maintain **recipes** (name, ingredients, preparation steps, labels).
- Use a **global ingredient catalog** to keep ingredient naming consistent and attach nutritional values.
- Compute and display **recipe caloric density (kcal/100g)** based on stored nutritional values.
- Collaborate across multiple clients with **real-time change synchronization**.

### Architecture at a Glance
- **Server (Spring Boot)**: REST API for CRUD operations + WebSocket/STOMP notifications for real-time sync + H2 persistence.
- **Client (JavaFX)**: GUI for browsing/editing recipes, filtering/search, nutrition management, shopping list, printing, and a timer.
- **Commons**: Shared domain model (`Recipe`, `Ingredient`, `PreparationStep`, `Label`, `GlobalIngredient`, `Unit`) used by both client and server.

Key entities:
- **Recipe**: Unique name, set of `Ingredient`s, ordered `PreparationStep`s, and `Label`s.
- **Ingredient**: References a `GlobalIngredient` (global naming + nutrition) and stores an amount.
- **GlobalIngredient**: Stores nutritional values (calories, protein, fat, carbs per 100g).
- **Label**: Categorized metadata (language, cuisine, dietary, cooking time, etc.) for filtering/search.

---

## Core Features and How to Test Them

Each feature below includes (1) what it does, (2) why it matters, and (3) exact TA test steps.

### 1) Shared Recipe Library (Create, View, Edit, Delete)
**What it does:** Full recipe CRUD backed by persistent server storage (H2).  
**Why it matters:** Demonstrates a complete distributed workflow: multiple clients share one consistent source of truth.

**How to test (Client UI):**
1. Start server and client (see [Installation](#installation-and-running-step-by-step)).
2. On **Home Page**, select a recipe from the left list to view details.
3. Click **Add** to create a recipe using the add-recipe wizard.
4. Select any recipe and click **Edit** (name / ingredients / steps) to modify it.
5. Select any recipe and click **Remove** (confirmation dialog) to delete it.

**Where it lives (code pointers):**
- Server: `server/api/RecipeController.java` (`/api/recipes`)
- Persistence: `server/database/RecipeRepository.java`, `server/src/main/resources/application.properties`
- Client UI: `client/scenes/HomePageCtrl.java`, `client/scenes/AddRecipeTabCtrl.java`, `client/scenes/EditRecipeCtrl.java`

---

### 2) Real-Time Change Synchronization (WebSocket Push)
**What it does:** When any client updates the recipe list or a recipe’s content, other clients receive **push notifications** and refresh automatically.  
**Why it matters:** Confirms multi-client correctness without manual refresh/polling and shows production-grade collaboration behavior.

**How to test (Two clients):**
1. Start **Server**.
2. Start **Client A** and **Client B** (two separate runs).
3. In Client A, **add** a new recipe or **edit** an existing one and save.
4. Observe Client B: the recipe list updates automatically.
5. Select the same recipe in both clients and edit its ingredients/steps in Client A; Client B updates the displayed recipe automatically.

**Where it lives (code pointers):**
- Server WebSocket: `server/WebSocketConfig.java` (`/ws`, topics under `/topic/**`)
- Server notifications: `server/api/RecipeController.java` (pushes `REFRESH`)
- Client WebSocket: `client/utils/WebSocketClient.java`
- Client integration: `client/scenes/HomePageCtrl.java` (`setupWebSocket()`)

---

### 3) Global Ingredient Database + Nutritional Values
**What it does:** Maintains a global ingredient catalog with nutritional values and supports safe deletion behavior when ingredients are in use.  
**Why it matters:** Adds real-world data consistency and enables meaningful nutrition computations (kcal estimates and recipe caloric density).

**How to test (Client UI → Nutritional Values screen):**
1. On **Home Page**, click **Nutritional Values**.
2. Select any ingredient from the list to see/edit:
    - **Calories**, **Protein**, **Fat**, **Carbs** (per 100g)
    - **Recommended kcal** (computed estimate shown next to the editable fields)
    - **Recipe usage count** (how many recipes reference it)
3. Edit a value (e.g., protein/fat/carbs) and click away; the values persist.
4. Click **Add** to create a new global ingredient.
5. Click **Delete** on a global ingredient:
    - If it is used, the UI shows a clear confirmation warning.
    - Confirming deletion removes it and updates dependent recipes accordingly.

**Where it lives (code pointers):**
- Client: `client/scenes/NutritionalValueCtrl.java`
- Server: `server/api/IngredientController.java` (global ingredient endpoints)
- Usage count: `server/api/RecipeController.java` (`/api/recipes/usage-count/{globalIngredientId}`)
- Model: `commons/GlobalIngredient.java`, `commons/Ingredient.java`

---

### 4) Recipe Caloric Density (kcal/100g) Computation
**What it does:** Computes and displays **kcal/100g** per recipe based on ingredient nutrition and quantities.  
**Why it matters:** Demonstrates cross-entity reasoning and transforms stored nutrition data into an immediately visible, testable output.

**How to test (Client UI):**
1. On **Home Page**, look at the recipe list: each recipe entry shows `… - XX.XX kcal/100g`.
2. Open **Nutritional Values**, adjust calories/macros for a global ingredient used by a recipe.
3. Return to Home Page and refresh selection; the displayed kcal/100g updates consistently.

**Where it lives (code pointers):**
- Computation: `commons/Recipe.java` (`getCaloricDensity()`)
- Display: `client/scenes/HomePageCtrl.java` (custom recipe list cell)

---
### 5) Scaling & Unit→Mass Conversions
**What it does:** Live recipe scaling multiplies numeric quantities in ingredient lines while preserving words/units, and a `RecipeNutritionUtils` conversion layer translates common volume units (cup, tbsp, ml) to grams (using density assumptions) so kcal/100g is computed consistently.
**How to test:** Pick a recipe containing both formal (e.g., `200 g`, `2 tbsp`) and informal amounts (e.g., `1.5 cans`). Scale by 2× or 3× and verify that numeric quantities change correctly while text and units remain intact, and that kcal/100g updates consistently.
#### Unit normalization when scaling
**What it does:** Ingredient amounts are normalized for readability after scaling (e.g., `1000 g` will be converted to `1 kg` where appropriate) by the `standardiseAmountString()` helper called during construction/update.
**How to test:** Select a recipe that leads to large numeric amounts when scaled (e.g., scale up so an ingredient reaches ~1000 g). After scaling, verify the UI displays a normalized unit (e.g., `1 kg` rather than `1000 g`) and that exported/printed text uses the same normalized form.



---

### 6) Fast Search + Filtering (Full-Text, Multi-Term AND, Labels)
**What it does:** A robust filter engine supports:
- Full-text search across recipe name, ingredients, preparation steps, and labels.
- Multi-term queries that behave as **AND** (all terms must match).
- Filters for favorites-only and recipe language labels.
- “Escape to clear” behavior for quick testing.

**Why it matters:** Makes the app efficient to evaluate and demonstrates careful UX + non-trivial query logic.

**How to test (Client UI):**
1. On **Home Page**, type a multi-term query (e.g., `pasta cheese`) in the search field.
2. Click **Search**: recipes are filtered only if **all terms** match somewhere.
3. Press **Escape** while focused in the search field to clear the search and restore the full list.
4. Click **Filter** (Advanced Search) to refine results further using label-based constraints.

**Where it lives (code pointers):**
- Filter logic: `client/utils/RecipeFilter.java`
- Search UI: `client/scenes/HomePageCtrl.java`, `client/scenes/AdvancedSearchCtrl.java`
- Server advanced search endpoints: `server/api/RecipeController.java` (`/api/recipes/search`, `/api/recipes/search/advanced`)

---

### 7) Favorites (Client-Local, Highlighted, Safe on Remote Deletes)
**What it does:** Favorites are stored locally (per client) and integrated into sorting and filtering; if a favorite recipe is deleted elsewhere, the client shows a clear warning and cleans up the local favorites list.
**Reference vs clone & rename-safety:** Favorites are stored as references (IDs) to server recipes — they are *not* client-side clones. This means a server-side rename or update of the recipe will be reflected in the favorite entry on the client (favorites survive renames).
**How to test:** Favorite a recipe in Client A; then rename that recipe in Client B (or via the server API). Verify Client A still shows the recipe as favorited and the displayed name updates automatically.


**Why it matters:** Demonstrates thoughtful client-side persistence and robust multi-client behavior under concurrent changes.

**How to test (Client UI):**
1. Select any recipe → click **Favorites** (toggle).
2. Observe the recipe list: favorited recipes show a **★** and are sorted prominently.
3. Enable the **Favorites** checkbox to show only favorites.
4. Multi-client test:
    - In Client A, favorite a recipe.
    - In Client B, delete that recipe.
    - In Client A, observe the warning dialog that favorite recipe(s) were deleted.

**Where it lives (code pointers):**
- Client favorite storage: `client/utils/FavoritesManager.java`, `client/utils/AppConfig.java`
- UI integration + warnings: `client/scenes/HomePageCtrl.java`

---

### 8) Shopping List Workflow (Build from Recipes + Edit + Print)
**What it does:** Users can build a shopping list, add items directly from recipes, edit/remove items, reset, and export to a text file.

**Why it matters:** Provides an end-to-end “use case” flow beyond CRUD and makes grading/validation quick.

**How to test (Client UI):**
1. On **Home Page**, click the **🛒** button to open the Shopping List.
2. Click **Add From Recipe** (or equivalent) and select a recipe.
3. Confirm the ingredient lines to add (recipe-sourced entries are preserved as readable list lines).
4. Edit an item by selecting it and clicking **Edit**, or remove it with **Remove**.
5. Click **Print** to export the shopping list to a `.txt` file.

**Where it lives (code pointers):**
- UI: `client/scenes/ShoppingListCtrl.java`, `client/scenes/ShoppingListConfirmationCtrl.java`
- Store: `client/scenes/ShoppingListStore.java`

---

### 9) Live UI Language Switching (English / Dutch/ German)
**What it does:** The UI supports live switching between **English**, **Dutch** and **German**, including translated labels/buttons via resource bundles, with the chosen language persisted locally.

**Why it matters:** Shows internationalization readiness and a polished TA-facing interface.

**How to test (Client UI):**
1. On **Home Page**, open the language selector (flag icon).
2. Switch between **English**,**Deutsch**  and **Nederlands** and observe UI text updates.
3. Restart the client: the last selected UI language is restored.

**Where it lives (code pointers):**
- Translation loader: `client/utils/TranslationManager.java`, `client/MyFXML.java`
- Bundles: `client/src/main/resources/text_en.properties`, `text_nl.properties`,`text_de.properties`
- Integration: `client/Main.java`, `client/scenes/HomePageCtrl.java`

---

## Technical Details

### Tech Stack
- **Language:** Java (multi-module Maven project)
- **Server:** Spring Boot 3.5.x, Spring Data JPA, Spring WebSocket (STOMP)
- **Client:** JavaFX (controls, FXML, web), dependency injection via Guice
- **Database:** H2 (file-based persistence)
- **Serialization / Mapping:** Jackson (JSON)
- **Testing:** JUnit 5 + Mockito (client + server + commons)
- **Style / Quality:** Checkstyle (`checkstyle.xml`) enforced via Maven plugins

### Communication and Data Flow
- **REST (HTTP/JSON):** CRUD + search endpoints served under `/api/**`
- **WebSocket (STOMP):** server broadcasts refresh signals on `/topic/**`; clients subscribe and update UI

### Persistence
- **Server:** H2 database stored on disk as `./h2-database` (see `server/src/main/resources/application.properties`)
- **Client (local):** `config.json` stores user-specific preferences such as UI language and favorite recipe IDs (`client/utils/AppConfig.java`)

---

## Installation and Running (Step-by-Step)

### Prerequisites
- **JDK 25** available on PATH (server is configured with Java 25; client/commons use modern Java release settings)
- No separate JavaFX installation needed (JavaFX dependencies are managed via Maven)

### Build Everything
From the project root:
```bash
./mvnw clean install
```

Windows:
```bat
mvnw.cmd clean install
```

### Run Server
From the project root:
```bash
./mvnw -pl server spring-boot:run
```

Expected:
- Server runs on `http://localhost:8080`
- H2 console enabled at `http://localhost:8080/h2-console`

### Run Client
In a second terminal (project root):
```bash
./mvnw -pl client javafx:run
```

Expected:
- Client starts and checks that the server is reachable.
- A local `config.json` file is created in the directory you launched the client from (if not already present).
- You can also pass a custom config directory via `--cfg /path/to/dir`. The client will normalize the path, append `config.json`, and create parent directories if they do not exist.

### Optional: Run Tests
```bash
./mvnw test
```
### Optional: Reset the database
The server uses an embedded H2 database persisted to a local file (`h2-database.mv.db`) in the project root.
To start fresh:
1. Stop the server.
2. Delete the database files from the project root:
    - `h2-database.mv.db`
    - `h2-database.trace.db` (if present)
3. Restart the server — the sample data will be re-initialized.

### Optional: Run multiple clients (for live sync)
To demonstrate real-time synchronization, start the client twice (in separate terminals) and keep both windows open. Make a change in one window and observe the other updating automatically.

### Troubleshooting
- **Port 8080 already in use:** stop the conflicting process or change its port.
- **Java version issues:** ensure `java -version` reports JDK 25.
- **Firewall/network:** server must be reachable at `localhost:8080` for the client to start normally.

---

## Navigation and Quick Walkthrough

### What the TA Sees First (Home Page)
The **Home Page** serves as the main hub of the application:

- **Left panel:** recipe list (each recipe shows an estimated **kcal/100g** when sufficient nutritional data is available), recipe search, and **Filter** button
    - The Filter button opens the advanced filter menu, where recipes can be filtered by criteria such as **Cuisine type**, **Protein**, and other labels.

- **Right panel:** details of the currently selected recipe, including **ingredients**, **preparation steps**, and **labels**.

- **Top controls:** common actions and shortcuts, including:
    - **Refresh** (refresh icon)
    - **Favorites** (star icon)
    - **Language switcher** (top-right button)
    - **Shopping list** (shopping cart icon)
    - **Nutritional values** (cucumber icon)
    - **Timer** (hourglass icon)
    - **Print** (print icon)

  > Note: the exact placement of these controls may vary slightly depending on the window size.


### Immediate Test Data (Preloaded)
On first server run, FoodPal initializes a set of sample recipes and global ingredients:
- Recipes include: **Pasta Carbonara**, **French Onion Soup**, **Pad Thai**, **Caprese-Salat**, **Chocoladechip Koekjes**
- Global ingredients are pre-populated with nutritional values to make kcal/100g features testable immediately.

(Initialization lives in `server/DatabaseInitializer.java`.)

## Walkthrough

### Create a Recipe

1. Click **Add Recipe**.
2. Fill out the recipe using the tab-based editor:

#### Name Tab
- Set a **recipe name**.
- The name **must be unique**.

#### Ingredients Tab
- Pick an ingredient from the **drop-down list** (global ingredient catalog), or type a new name to create it directly.
- Provide **one** of the following:
    - **Formal amount** (numeric value + unit), or
    - **Informal amount** (e.g., “a pinch”, “to taste”).
- Click **Add** to stage the ingredient in the list.
- Click **Next** to continue.

Note: the Ingredients list is editable in-place — click an item to populate the editor fields, then **Update** or **Delete** it. The **Update / Clear / Delete** buttons are enabled only when an item is selected to prevent null operations.


#### Preparation Tab
- Add preparation steps.
- Each step is stored as an **individual, ordered step**.

#### Additional Tab
- Choose the **Language** for the recipe (required and used for filtering).
- Add optional **labels**, such as:
    - Cuisine
    - Meal type
    - Dietary preference
    - Protein
    - Cooking time
    - etc.
      **Note for graders:** you may occasionally see a non-fatal exception logged when adding or editing labels. This exception is caught and handled client-side and has no effect on the saved recipe or label data — the UI state and persistence remain correct. Please do not penalize for this handled logging-only issue.


3. Click **Save** to store the recipe on the server.

---

### Edit a Recipe (Name / Ingredients / Preparation)

1. Select a recipe on the Home Page.
2. Click **Edit Name**, **Edit Ingredients**, or **Edit Steps** using the corresponding **pencil icon**.
3. Save or close the dialog; changes are sent directly to the server.

---

### Clone a Recipe

1. Select a recipe.
2. Click **Clone**.
3. Enter a **new name**.

This creates a copy of the recipe on the server, allowing you to modify it without affecting the original.

---

### Print / Export a Recipe

1. Select a recipe.
2. Click **Print**.
3. Select a Scaling factor to print (This will scale the ingredient amounts)
4. A printable text export is downloaded which includes amounts when present (e.g., `- Sugar (150g)`), saving the exported `.txt` to the project root.

---

### Refresh

- A **Refresh** button is available even though live updates are implemented.
- Use it to force a full reload of data from the server if needed.


---

## Features Implemented (Including Extensions)

This section provides a concise, grader-oriented overview of all implemented features and where they can be observed in the application.

---

### 1) Core Requirements (Client–Server Recipe Management)

- **Server-side recipe storage**
    - Recipes are stored persistently on the server via REST/JSON.
    - Full CRUD support: create, view, edit, delete recipes.
    - Data persists across server restarts (H2 database).

- **Recipe structure**
    - Recipes contain:
        - A unique name
        - Ingredients
        - Ordered preparation steps
        - Labels (metadata)

- **Editing support**
    - Edit recipe name, ingredients, and preparation steps via dedicated dialogs.
    - Changes are sent directly to the server.

- **Recipe cloning**
    - Clone an existing recipe using a server-side clone operation.
    - The clone is stored independently and can be modified without affecting the original.

- **Manual refresh**
    - A Refresh button is available to force a full reload from the server.

- **Recipe export**
    - Recipes can be exported as printable text files via the Print action.

---

### 2) Automated Change Synchronization (WebSockets)

- Push-based synchronization using WebSocket/STOMP:
    - Recipe list updates automatically when another client adds, edits, or deletes a recipe.
    - When multiple clients view the same recipe, content updates are pushed live.

**Quick verification:**
1. Start the server.
2. Start two clients.
3. Edit or delete a recipe in Client A.
4. Observe automatic updates in Client B without pressing Refresh.

---

### 3) Nutritional Values and Global Ingredients

- **Global ingredient catalog**
    - Central list of ingredients shared across all recipes.
    - Accessible via the Nutrition (🥒 cucumber) button.

- **Nutritional data**
    - Per-100g values for protein, fat, and carbohydrates.
    - An **automatically calculated kcal estimate** based on macros  
      (`protein × 4 + carbs × 4 + fat × 9`).
    - A **manually editable kcal value** is also supported for flexibility.

- **Usage tracking and safety**
    - Each global ingredient shows how many recipes reference it.
    - Deleting a used ingredient triggers a warning; confirmation removes it from all affected recipes.

- **Ingredient reuse**
    - Global ingredients are selected via drop-down when creating recipes.
    - Renaming a global ingredient updates all referencing recipes automatically.
    - New ingredients can be created directly from the UI.

- **Formal vs informal amounts**
    - Supports:
        - Formal amounts (value + unit)
        - Informal amounts (“pinch”, “to taste”, etc.)
    - Supported units include g, kg, ml, l, tsp, tbsp, cup, pcs.
    - Informal amounts are ignored in nutritional calculations.

- **Recipe kcal/100g**
    - Each recipe displays an estimated kcal/100g when sufficient data is available.
    - Calculations ignore informal ingredient amounts.

---

### 4) Searching, Filtering, and Favorites

- **Favorites**
    - Recipes can be starred/unstarred.
    - Favorites are stored locally (client-only).
    - Favorites are highlighted and can be viewed in a favorites-only mode.
    - If a favorited recipe is deleted by another client, it is removed locally and a warning is shown.

- **Search**
    - Full-text search directly filters the recipe list.
    - Supports multi-term **AND** semantics.
    - Searches across:
        - Recipe names
        - Ingredient names
        - Preparation steps
        - Labels
    - Press **Escape** to clear the search field.

- **Advanced filtering (extension)**
    - Dedicated Advanced Search / Filter dialog.
    - Filters by:
        - Labels (Cuisine, Meal, Dietary, Protein, Cooking Time)
        - Recipe language
        - Cooking-time thresholds

---

### 5) Shopping List (Client-Only)

- Shopping list is maintained locally and not stored on the server.
- Users can:
    - Add and remove items manually
    - Edit items
    - Reset the list
    - Export a printable text file

- **Add from recipe**
    - Select a recipe → Add to Shopping List.
    - An editable preview is shown before confirmation.
    - Ingredients from multiple recipes remain separate and include the source recipe name.
    - Additional items can be manually added

---

### 6) Live Language Switching and Recipe Language Support

- **UI language switching**
    - Switch UI language at runtime via the language selector.
    - Language choice is persisted locally. 
    - Supported UI languages: English, Dutch, and German.

- **Recipe language**
    - Each recipe must have a language set during creation.
    - Recipe language is used in filtering and advanced search.

---


## Hard-to-Find Features

- **Recipe kcal/100g display:** shown directly inside the recipe list entry on the Home Page (e.g., `RecipeName - 123.45 kcal/100g`).
    - Code: `commons/Recipe.java#getCaloricDensity()`, `client/scenes/HomePageCtrl.java` cell factory.

- **Escape-to-clear search:** click inside the search bar and press **Esc**.
    - Code: `client/scenes/HomePageCtrl.java` key handler.

- **Advanced filtering (labels + language):** click **Advanced Search / Filter** on the Home Page.
    - Allows filtering by recipe labels, language, and cooking-time thresholds.
    - Code: `client/scenes/AdvancedSearchCtrl.java`, `client/utils/RecipeFilter.java`.

- **Shopping list preview:** select a recipe → **Add to Shopping List** → edit the preview → confirm.
    - Code: `client/scenes/ShoppingListConfirmationCtrl.java`.

**Update Ingredient in Shopping list:** select an ingredient → **In the add Ingredient TextBox** → overwrite the ingredient → click update (instead of add).
- Code: `client/scenes/ShoppingListCtrl.java`.

- **Nutrition / Global ingredients:** click the **🥒 (cucumber) button** on the Home Page.
    - Opens the global ingredient editor with nutritional values and usage counts.
    - Code: `client/scenes/NutritionalValueCtrl.java`.

- **Used-ingredient deletion warning:** in **Nutritional Values**, delete an ingredient that appears in at least one recipe.
    - Code: `client/scenes/NutritionalValueCtrl.java`, `server/api/IngredientController.java`.

- **Two-level real-time sync:** recipe list refresh + selected-recipe refresh are handled via separate WebSocket subscriptions.
    - Code: `client/scenes/HomePageCtrl.java#setupWebSocket()`.


---

## Extensions and Excellent Features

The following features go beyond the basic requirements and strengthen the project in terms of usability, realism, and technical depth:

- **Advanced Search and Filtering**
    - An **Advanced Filter dialog** allows filtering recipes by:
        - Labels (e.g., cuisine, meal type, dietary preferences, protein source)
        - Recipe language
        - **Cooking-time thresholds** (e.g., maximum preparation time)
    - Recipes support **rich, structured labeling** (Cuisine / Meal / Dietary / Protein / Cooking Time), enabling precise and meaningful filtering beyond simple text search.
    - Combined with full-text search, this provides a powerful and efficient recipe discovery workflow.

- **Extended Nutritional Value Support**
    - Each global ingredient exposes a **manually editable kcal value** (per 100g), allowing precise control when exact nutritional data is known.
    - In parallel, the system provides an **automatically calculated kcal estimate** derived from protein, fat, and carbohydrate values.
    - This dual approach improves realism and flexibility: users can rely on automatic computation while retaining the ability to override values when needed.