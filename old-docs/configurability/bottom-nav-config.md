
#### Bottom Navigation Configuration
  - **Navigation Item**
  - **Action Configuration**
  - **Handle Navigation Action**
  - **JSON Specification Provider**

##### Navigation Item:
Navigation item will be added in `register_view_configuration.json` in the `bottomNavigationOptions` JSON array section. i.e as shown in the below JSON snippet. All keys are required `id`, `title`, `icon`, `action`.

```JSON
[
  {
    "showBottomMenu": true,
    "bottomNavigationOptions": [
      {
        "id": "menu_item_clients",
        "title": "Clients",
        "icon" : "ic_users",
        "action": {
          "type": "switch_fragment",
          "tag": "PatientRegisterFragment",
          "isRegisterFragment": true,
          "isFilterVisible": false,
          "toolbarTitle": null
        }
      }
    ]
  }
]
```

##### Action Configuration:
To implement a navigation action you will need to create a class that will hold the dynamic data. All the navigation classes will implement a `NavigationAction` interface and the class will also be annotated with `@Serializable` and `@SerialName("any_action_name")` annotation. For example:

```Kotlin
@Serializable
@SerialName("switch_fragment")
data class ActionSwitchFragment(
  val tag: String,
  val isRegisterFragment: Boolean,
  val isFilterVisible: Boolean,
  val toolbarTitle: String?
) : NavigationAction
```

##### Handle Navigation Action:
The navigation action will handle in any child actiivty of `BaseRegisterActivity` class by overriding a method called `onBottomNavigationOptionItemSelected(item: MenuItem, viewConfiguration: RegisterViewConfiguration)`.

##### JSON Specification Provider
After the configuraiton of navigation item the navigation action must class must be register in json specification provider class i.e `AncJsonSpecificationProvider` / `QuestJsonSpecificationProvider` for example:

```Kotlin
fun getJson(): Json {
  val module = SerializersModule {
    polymorphic(NavigationAction::class) {
      subclass(ActionSwitchFragment::class)
    }
  }

  return Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
    isLenient = true
    serializersModule = module
  }
}
```
