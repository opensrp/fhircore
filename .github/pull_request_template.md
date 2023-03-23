**IMPORTANT: Where possible all PRs must be linked to a Github issue**

Fixes #[issue number] or Closes #[issue number]

**Engineer Checklist**
- [ ] I have written **Unit tests** for any new feature(s) and edge cases for bug fixes
- [ ] I have added any strings visible on UI components to the `strings.xml` file
- [ ] I have updated the  [CHANGELOG.md](./CHANGELOG.md) file for any notable changes to the codebase
- [ ] I have run `./gradlew spotlessApply` and `./gradlew spotlessCheck` to check my code follows the project's style guide
- [ ] I have built and run the FHIRCore app to verify my change fixes the issue and/or does not break the app 


**Code Reviewer Checklist**
- [ ] I have verified **Unit tests** have been written for any new feature(s) and edge cases
- [ ] I have verified any strings visible on UI components are in the `strings.xml` file
- [ ] I have verifed the [CHANGELOG.md](./CHANGELOG.md) file has any notable changes to the codebase
- [ ] I have verified the solution has been implemented in a configurable and generic way for reuseable components
- [ ] I have built and run the FHIRCore app to verify the change fixes the issue and/or does not break the app
 