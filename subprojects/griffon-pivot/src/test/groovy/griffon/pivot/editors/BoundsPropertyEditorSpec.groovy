/*
 * Copyright 2008-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package griffon.pivot.editors

import org.apache.pivot.wtk.Bounds
import spock.lang.Specification
import spock.lang.Unroll

import java.beans.PropertyEditor

@Unroll
class BoundsPropertyEditorSpec extends Specification {
    void "Bounds format '#format' should be equal to #bounds"() {
        setup:

        PropertyEditor editor = new BoundsPropertyEditor()

        when:
        editor.value = format

        then:

        bounds == editor.value

        where:
        bounds                     | format
        null                       | null
        null                       | ''
        null                       | ' '
        null                       | []
        null                       | [:]
        new Bounds(10, 20, 30, 40) | '10,20,30,40'
        new Bounds(10, 20, 30, 40) | '10, 20, 30, 40'
        new Bounds(10, 20, 30, 40) | ' 10, 20, 30, 40'
        new Bounds(10, 20, 30, 40) | ' 10, 20, 30, 40 '
        new Bounds(10, 20, 30, 40) | [10, 20, 30, 40]
        new Bounds(10, 20, 30, 40) | ['10', '20', '30', '40']
        new Bounds(10, 20, 30, 40) | [x: 10, y: 20, width: 30, height: 40]
        new Bounds(10, 20, 30, 40) | [x: '10', y: '20', width: '30', height: '40']
        new Bounds(10, 20, 30, 40) | [x: 10, y: 20, w: 30, h: 40]
        new Bounds(10, 20, 30, 40) | [x: '10', y: '20', w: '30', h: '40']
        new Bounds(10, 20, 30, 40) | new Bounds(10, 20, 30, 40)
        new Bounds(0, 0, 0, 0)     | [foo: 10, bar: 20]
    }

    void "Invalid bounds format '#format'"() {
        setup:

        PropertyEditor editor = new BoundsPropertyEditor()

        when:
        editor.value = format

        then:

        thrown(IllegalArgumentException)

        where:
        format << [
            'garbage',
            '1, 2, 3',
            '1, 2, 3, 4, 5',
            [1, 2, 3],
            [1, 2, 3, 4, 5],
            [x: 'a'],
            [y: 'b'],
            new Object()
        ]
    }
}