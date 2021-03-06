<?php

class SuperClass
{
    protected function __construct() {}

    public function doSomething1() {}

    public function doSomething2($x) {}

    public function doSomethingElse1($x, $y, $z) {}

    public function doSomethingElse2($x, $y, $z) {}

    public function withClassConstants($x = __CLASS__, $y = __METHOD__) {}
}

class ChildClass extends SuperClass
{
    public function __construct() {
        parent::__construct();
    }

    public function doSomethingElse1($x, $y, $z) {
        parent::doSomethingElse1($z, $y, $x);
    }

    public function doSomethingElse2($x, $y, $z) {
        parent::doSomethingElse1($x, $y, (int)$z);
    }

    public function withClassConstants($x = __CLASS__, $y = __METHOD__) {
        parent::withClassConstants($x, $y);
    }
}