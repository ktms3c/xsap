package com.example.xssscanner.scanner.payload

/** Context hint used to pick a payload shape. Not the same as the
 *  domain-level reflection "context" string (html_body/attribute/js_string/
 *  url/css) stored on XssFinding — this is an input hint, that is an output
 *  classification. */
enum class PayloadContext { GENERIC, HTML_BODY, ATTRIBUTE, JS_STRING, URL, CSS }
