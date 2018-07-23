package com.hoten.delaunay.geom

import spock.lang.Specification

class GenUtilsTest extends Specification {

    def "closeEnough should return true"() {
        expect:
        GenUtils.closeEnough(d1, d2, inaccuracy)

        where:
        d1    | d2    | inaccuracy
        1.0   | 2.0   | 1.0
        0.001 | 0.003 | 0.005
    }

    def "closeEnough should return false"() {
        expect:
        !GenUtils.closeEnough(d1, d2, inaccuracy)

        where:
        d1      |   d2      |   inaccuracy
        1.0     |   2.0     |   0.9
        0.001   |   0.003   |   0.001
    }
}
