aem-htl-ide-api
===============

The Adobe HTL IDE API bundle provides a JSON description of the [HTL Java Use-API](https://github.com/Adobe-Marketing-Cloud/htl-spec/blob/1.2/SPECIFICATION.md#41-java-use-api) Objects stored in the content repository.

This JSON description provides basic auto-complete features to the [AEM Brackets Extension](https://github.com/Adobe-Marketing-Cloud/aem-brackets-extension).

For example:

`/whatever/existing/path.sightlyBeans.json` returns

```json
{
    "com.adobe.beagle.beans.Tabs": {
        "members":[
            {
                "name":"horizontal",
                "desc":"()Z",
                "sign":"isHorizontal()Z"
            },
            {
                "name":"id",
                "desc":"()Ljava/lang/String;",
                "sign":"getId()Ljava/lang/String;"
            }
        ]
    }
}
```

Note that for the moment only the `name` key should be used as the rest is subject to changes.
