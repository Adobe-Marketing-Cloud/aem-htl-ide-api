/*******************************************************************************
 * Copyright (c) 2014 Adobe Systems Incorporated. All rights reserved.
 *
 * Licensed under the Apache License 2.0.
 * http://www.apache.org/licenses/LICENSE-2.0
 ******************************************************************************/
package com.adobe.aem.sightly.ide;

import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;

/**
 * SightlyBeanFinder
 */

public interface SightlyBeanFinder {
    public void writeUseBeans(JSONWriter writer) throws JSONException;
}
