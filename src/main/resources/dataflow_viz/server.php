<?php
// Set the JSON header
header("Content-type: text/json");

// The x value is the current JavaScript time, which is the Unix time multiplied by 1000.

$x =1;
// The y value is a random number
$y = 50+rand(0, 20);

$name='random';

// Create a PHP array and echo it as JSON
$ret = array($name, $x, $y);

//print $ret;
echo json_encode($ret);
