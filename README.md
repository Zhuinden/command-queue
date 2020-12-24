# Command Queue

The command queue lets you register a single receiver, and while there is no receiver, the commands are enqueued.

## Using Command Queue

In order to use Command Queue, you need to add jitpack to your project root gradle:

    buildscript {
        repositories {
            // ...
            maven { url "https://jitpack.io" }
        }
        // ...
    }
    allprojects {
        repositories {
            // ...
            maven { url "https://jitpack.io" }
        }
        // ...
    }


and add the dependency to your module level gradle.

    implementation 'com.github.Zhuinden:command-queue:1.2.0'

## Sample code

See sample [here](https://github.com/Zhuinden/command-queue/tree/302edf809545e912c38db0865164f418d24f38d0/command-queue-sample).

Please note that CommandQueue is used as internal implementation detail for both `EventEmitter` and `LiveEvent`, if those suit your needs better.

## License

    Copyright 2018 Gabor Varadi

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
