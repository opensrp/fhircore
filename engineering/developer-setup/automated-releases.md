# Releases

As part of integrating Continuous Delivery(CD) into the development lifecycle, CI is set up to generate an APK.

The configuration requires the tag to:

* Have a _prefix_ in the format `v[0-9]+.[0-9]+.[0-9]+[0-9a-zA-Z.-]+`
* Followed by the specific product flavor name _e.g. quest, g6pd, ecbis e.t.c_
* One can then add a suitable _suffix_ depending on the release e.g. `-alpha`, `-rc`, `-beta` e.t.c

The following are all valid tags that will trigger the generation of a release APK

> * v1.2.0-quest
> * v1.2.0-quest-alpha
> * v1.2.0-g6pd-beta
> * v1.2.0-quest-rc1

**Note:** e.g. when creating a tag for the _quest version 1.2.0_, use the command:

```
git tag -a v1.2.0-quest -s  && git push origin v1.2.0-quest
```

When you run the command, you will be prompted to _add a message_. The message should be of the format:

| Template                                                             | Sample                                                                                                                  |
| -------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------- |
| <pre><code> TITLE 
 - Release note 1
 - Release note 2
</code></pre> | <pre><code> BETA RELEASE 
 - Adds Login by PIN functionality 
 - Fixes sync bug causing crash on install 
</code></pre> |

**Note:** For convention, the TITLE should be _Capitalized_.

## Deleting a tag

Sometimes you may want to delete a tag, e.g. if you push an incorrect tag, need to update the tag message etc.

For example, to delete the v1.2.0-quest tag run the command:

```
git push --delete origin v1.2.0-quest && git tag --delete v1.2.0-quest
```
