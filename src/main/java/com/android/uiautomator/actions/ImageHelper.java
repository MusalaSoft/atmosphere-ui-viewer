/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.uiautomator.actions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;

public class ImageHelper {

    public static ImageDescriptor loadImageDescriptorFromResource(String path) {
        InputStream inputStream = null;

        try {
            inputStream = new FileInputStream(new File(path));
        } catch (FileNotFoundException e1) {
            return null;
        }

        if (inputStream != null) {
            ImageData[] data = null;
            try {
                data = new ImageLoader().load(inputStream);
            } catch (SWTException e) {
            } finally {
                try {
                    inputStream.close();
                } catch (IOException e) {
                }
            }
            if (data != null && data.length > 0) {
                return ImageDescriptor.createFromImageData(data[0]);
            }
        }
        return null;
    }
}
