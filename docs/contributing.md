# How to Contribute

For us to accept your patches and contributions to this project there are a few small guidelines you need to follow.

## Code commits

First, be aware of ABC - Always Be Committing! Make frequent and small commits.

_Second, the 7 rules of writing great commit messages:_

- Separate subject from body with a blank line
- Limit the subject line to 50 characters
- Capitalize the subject line
- Do not end the subject line with a period
- Use the imperative mood in the subject line
- Wrap the body at 72 characters
- Use the body to explain what and why vs. how

_Commits Rule of thumb:_

- Always commit at the end of the day before leaving the office
- Commits should be specific on the change(s), additions, fixes e.t.c
- Use the imperative form when writing your commit messages e.g. Make Class X Method Y return Type String or instead of Changed Method Y in Class X to return Y

e.g. for version source control

[Version control best practices](https://blog.rainforestqa.com/2014-05-28-version-control-best-practices/)
[Pointers from Linus](https://github.com/torvalds/linux/pull/17#issuecomment-5659933)

## Pull Requests

The requirements of making a Pull Request are enforced via a template, this sectin describes how to fill each section

`Fixes #[issue number]`

It is recommended to link al PRs to a Github issue. The section will appear on the template like this, replace the [issue number] with the actual issue number e.g. 230 and Github will convert it automatically to a link

`**Type** _Choose one: (Bug fix | Feature | Documentation | Testing | Code health | Release | Other)`\_

Here you are meant to select one type that best fits the PR you are trying to make

You must always please perform the actions on the check list below before you mark them as done.

**Checklist**

- [ ] I have written **Unit tests** for any new feature(s) and edge cases for bug fixes
- [ ] I have added any strings visible on UI components to the `strings.xml` file
- [ ] I have updated the [CHANGELOG.md](./CHANGELOG.md) file for any notable changes to the codebase
- [ ] I have run `./gradlew spotlessApply` and `./gradlew spotlessCheck` to check my code follows the project's style guide
- [ ] I have built and run the fhircore app to verify my change fixes the issue and/or does not break the ap

## Code reviews

All submissions, including submissions by project members, require code review. We use GitHub pull requests for this purpose.

We require all checks in the PR template to be completed before submitting .

Every time a feature is updated or a bug is fixed, a Peer review or Code review is required before the code/patch is merged to master.

**The Developer is required to:**

- Create a PR(Pull Request) to the parent branch, usually master. However it could any other depending on the project set up e.g. develop branch
- Assign 1 or 2 reviewers to the PR
- Notify them. While Github does have notifications for the above actions. It is recommended to notify the reviewers via slack you assigned especially depending on urgency. Before doing this please ensure the status checks on the PR have all passed.

**The Reviewer is required to:**

- Ensure all the CI status checks as set up on Github pass e.g. Build, Codacy, Code coverage
- Perform a manual review of the code. If anything requires an update, add a comment to the corresponding lines on the file within the Github PR
- Check out the code on your local work station, deploy and :
  - Test the new feature while referencing the corresponding spec/Github issue
  - Test any other related feature the update might affect
  - Test for any possible backward compatibility breaks
    If all the above pass, then the reviewer approves the PR. Note that though Github does have notifications for the above actions, It is recommended to notify the requesting developer once you are done reviewing incase the update/patch is an urgent one.

**The developer requesting the PR then follows these steps:**

- If there is feedback on the PR, please respond on the PR or make the changes requested before the PR can be approved. Quality PRs take time to merge in :)
- Merge the PR code from the issue branch to master
- Delete the issue branch

Please see [additional documentation on PRs here](https://github.com/opensrp/fhircore/issues/324).
