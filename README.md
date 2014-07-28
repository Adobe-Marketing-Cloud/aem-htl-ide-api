aem-sightly-ide-api
===============

The Adobe Granite Sightly IDE API bundle provides a JSON description of Sightly Java Use-API Objects stored in the content repository.

This JSON description provides basic auto-complete features to the AEM Sightly Brackets Extension.

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
