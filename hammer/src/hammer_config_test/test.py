#!/usr/bin/env python3
# -*- coding: utf-8 -*-
#
#  Unit tests for the hammer_config module.
#
#  Copyright 2017 Edward Wang <edward.c.wang@compdigitec.com>
import tempfile

import hammer_config

import unittest

import os


class HammerDatabaseTest(unittest.TestCase):

    def test_overriding(self):
        """
        Test that we can add a project first and technology after and still have it override.
        """
        db = hammer_config.HammerDatabase()
        db.update_project([{"tech.x": "foo"}])
        self.assertEqual(db.get_setting("tech.x"), "foo")
        db.update_technology([{"tech.x": "bar"}])
        self.assertEqual(db.get_setting("tech.x"), "foo")

    def test_unpacking(self):
        """
        Test that input configs get unpacked.
        """
        db = hammer_config.HammerDatabase()
        config = hammer_config.load_config_from_string("""
foo:
    bar:
        adc: "yes"
        dac: "no"
""", is_yaml=True)
        db.update_core([config])
        self.assertEqual(db.get_setting("foo.bar.adc"), "yes")
        self.assertEqual(db.get_setting("foo.bar.dac"), "no")

    def test_no_config_junk(self):
        """Test that no _config_path junk variables get left behind."""
        db = hammer_config.HammerDatabase()
        db.update_core([hammer_config.load_config_from_string("key1: value1", is_yaml=True)])
        db.update_technology([hammer_config.load_config_from_string("key2: value2", is_yaml=True)])
        db.update_project([hammer_config.load_config_from_string("key3: value3", is_yaml=True)])
        for key in hammer_config.HammerDatabase.internal_keys():
            self.assertFalse(db.has_setting(key), "Should not have internal key " + key)

    def test_meta_json2list(self):
        """
        Test that the meta attribute "json2list" works.
        """
        db = hammer_config.HammerDatabase()
        base = hammer_config.load_config_from_string("""
foo:
    flash: "yes"
    max: "min"
""", is_yaml=True)
        meta = hammer_config.load_config_from_string("""
{
    "foo.pipeline": "[\\"1\\", \\"2\\"]",
    "foo.pipeline_meta": "json2list"
}
    """, is_yaml=False)
        db.update_core([base, meta])
        self.assertEqual(db.get_setting("foo.flash"), "yes")
        self.assertEqual(db.get_setting("foo.max"), "min")
        self.assertEqual(db.get_setting("foo.pipeline"), ["1", "2"])

    def test_meta_dynamicjson2list(self):
        """
        Test that the meta attribute "dynamicjson2list" works.
        """
        db = hammer_config.HammerDatabase()
        base = hammer_config.load_config_from_string("""
foo:
    flash: "yes"
    max: "min"
    pipeline: "[]"
    pipeline_meta: "dynamicjson2list"
""", is_yaml=True)
        meta = hammer_config.load_config_from_string("""
{
    "foo.pipeline": "[\\"1\\", \\"2\\"]"
}
    """, is_yaml=False)
        db.update_core([base, meta])
        self.assertEqual(db.get_setting("foo.flash"), "yes")
        self.assertEqual(db.get_setting("foo.max"), "min")
        self.assertEqual(db.get_setting("foo.pipeline"), ["1", "2"])

    def test_meta_subst(self):
        """
        Test that the meta attribute "subst" works.
        """
        db = hammer_config.HammerDatabase()
        base = hammer_config.load_config_from_string("""
foo:
    flash: "yes"
    one: "1"
    two: "2"
""", is_yaml=True)
        meta = hammer_config.load_config_from_string("""
{
  "foo.pipeline": "${foo.flash}man",
  "foo.pipeline_meta": "subst",
  "foo.uint": ["${foo.one}", "${foo.two}"],
  "foo.uint_meta": "subst"
}
""", is_yaml=False)
        db.update_core([base, meta])
        self.assertEqual(db.get_setting("foo.flash"), "yes")
        self.assertEqual(db.get_setting("foo.pipeline"), "yesman")
        self.assertEqual(db.get_setting("foo.uint"), ["1", "2"])

    def test_meta_dynamicsubst(self):
        """
        Test that the meta attribute "dynamicsubst" works.
        """
        db = hammer_config.HammerDatabase()
        base = hammer_config.load_config_from_string("""
foo:
    flash: "yes"
    one: "1"
    two: "2"
style: "waterfall"
""", is_yaml=True)
        meta = hammer_config.load_config_from_string("""
{
  "foo.pipeline": "${foo.flash}man",
  "foo.pipeline_meta": "subst",
  "foo.reg": "Wire",
  "foo.reginit": "${foo.reg}Init",
  "foo.reginit_meta": "dynamicsubst",
  "foo.later": "${later}",
  "foo.later_meta": "dynamicsubst",
  "foo.methodology": "${style} design",
  "foo.methodology_meta": "dynamicsubst"
}
""", is_yaml=False)
        project = hammer_config.load_config_from_string("""
{
  "later": "later",
  "style": "agile"
}
""", is_yaml=False)
        db.update_core([base, meta])
        db.update_project([project])
        self.assertEqual(db.get_setting("foo.flash"), "yes")
        self.assertEqual(db.get_setting("foo.pipeline"), "yesman")
        self.assertEqual(db.get_setting("foo.reginit"), "WireInit")
        self.assertEqual(db.get_setting("foo.later"), "later")
        self.assertEqual(db.get_setting("foo.methodology"), "agile design")

    def test_meta_dynamicsubst_other_dynamicsubst(self):
        """
        Check that a dynamicsubst which references other dynamicsubst errors for now.
        """
        """
        Test that the meta attribute "dynamicsubst" works.
        """
        db = hammer_config.HammerDatabase()
        base = hammer_config.load_config_from_string("""
foo:
    flash: "yes"
    one: "1"
    two: "2"
    lolcat: ""
    twelve: "${lolcat}"
    twelve_meta: dynamicsubst
""", is_yaml=True)
        project = hammer_config.load_config_from_string("""
{
  "lolcat": "whatever",
  "later": "${foo.twelve}",
  "later_meta": "dynamicsubst"
}
""", is_yaml=False)
        db.update_core([base])
        db.update_project([project])
        with self.assertRaises(ValueError):
            print(db.get_config())

    def test_meta_append(self):
        """
        Test that the meta attribute "append" works.
        """
        db = hammer_config.HammerDatabase()
        base = hammer_config.load_config_from_string("""
foo:
    bar:
        adc: "yes"
        dac: "no"
        dsl: ["scala"]
""", is_yaml=True)
        meta = hammer_config.load_config_from_string("""
{
  "foo.bar.dsl": ["python"],
  "foo.bar.dsl_meta": "append",
  "foo.bar.dac": "current_weighted"
}
""", is_yaml=False)
        db.update_core([base, meta])
        self.assertEqual(db.get_setting("foo.bar.dac"), "current_weighted")
        self.assertEqual(db.get_setting("foo.bar.dsl"), ["scala", "python"])

    def test_repeated_updates(self) -> None:
        """
        Test that repeated updates don't cause duplicates.
        """
        db = hammer_config.HammerDatabase()
        base = hammer_config.load_config_from_string("""
a.b:
  c: []
""", is_yaml=True)
        meta = hammer_config.load_config_from_string("""
a.b.c: ["test"]
a.b.c_meta: append
""", is_yaml=True)
        db.update_core([base])
        self.assertEqual(db.get_setting("a.b.c"), [])
        db.update_project([meta])
        self.assertEqual(db.get_setting("a.b.c"), ["test"])
        db.update_technology([])
        self.assertEqual(db.get_setting("a.b.c"), ["test"])
        db.update_environment([])
        self.assertEqual(db.get_setting("a.b.c"), ["test"])

    def test_meta_prependlocal(self):
        """
        Test that the meta attribute "prependlocal" works.
        """
        db = hammer_config.HammerDatabase()
        base = hammer_config.load_config_from_string("""
foo:
    bar:
        adc: "yes"
        dac: "no"
        dsl: ["scala"]
        base_test: "local_path"
        base_test_meta: prependlocal
""", is_yaml=True, path="base/config/path")
        meta = hammer_config.load_config_from_string("""
{
  "foo.bar.dsl": ["python"],
  "foo.bar.dsl_meta": "append",
  "foo.bar.dac": "current_weighted",
  "foo.bar.meta_test": "local_path",
  "foo.bar.meta_test_meta": "prependlocal"
}
""", is_yaml=False, path="meta/config/path")
        db.update_core([base, meta])
        self.assertEqual(db.get_setting("foo.bar.dac"), "current_weighted")
        self.assertEqual(db.get_setting("foo.bar.dsl"), ["scala", "python"])
        self.assertEqual(db.get_setting("foo.bar.base_test"), "base/config/path/local_path")
        self.assertEqual(db.get_setting("foo.bar.meta_test"), "meta/config/path/local_path")

    def test_meta_transclude(self):
        """
        Test that the meta attribute "transclude" works.
        """
        # Put some text into the file.
        file_contents = "The quick brown fox jumps over the lazy dog"
        fd, path = tempfile.mkstemp(".txt")
        with open(path, "w") as f:
            f.write(file_contents)

        db = hammer_config.HammerDatabase()
        base = hammer_config.load_config_from_string("""
chips:
    potato: tuber
    bear: "yeah"
""", is_yaml=True, path="base/config/path")
        meta = hammer_config.load_config_from_string("""
{
  "chips.tree": "<path>",
  "chips.tree_meta": "transclude"
}
""".replace("<path>", path), is_yaml=False, path="meta/config/path")

        db.update_core([base, meta])

        # Trigger merge before cleanup
        self.assertEqual(db.get_setting("chips.potato"), "tuber")

        # Cleanup
        os.remove(path)

        self.assertEqual(db.get_setting("chips.bear"), "yeah")
        self.assertEqual(db.get_setting("chips.tree"), file_contents)

    def test_meta_transclude_prependlocal(self):
        """
        Test that the meta attribute "transclude" works with "prependlocal".
        """
        # Put some text into the file.
        file_contents = "The quick brown fox jumps over the lazy dog"
        local_path = "meta/config/path"
        fd, path = tempfile.mkstemp(".txt")
        with open(path, "w") as f:
            f.write(file_contents)

        db = hammer_config.HammerDatabase()
        base = hammer_config.load_config_from_string("""
chips:
    potato: tuber
    bear: "yeah"
""", is_yaml=True)
        meta = hammer_config.load_config_from_string("""
{
  "chips.tree": "<path>",
  "chips.tree_meta": ["transclude", "prependlocal"]
}
""".replace("<path>", path), is_yaml=False, path=local_path)

        db.update_core([base, meta])

        # Trigger merge before cleanup
        self.assertEqual(db.get_setting("chips.potato"), "tuber")

        # Cleanup
        os.remove(path)

        self.assertEqual(db.get_setting("chips.bear"), "yeah")
        self.assertEqual(db.get_setting("chips.tree"), os.path.join(local_path, file_contents))

    def test_meta_transclude_subst(self):
        """
        Test that the meta attribute "transclude" works with "subst".
        """
        # Put some text into the file.
        file_contents = "I like ${food.condiment} on my ${food.dish}."
        file_contents_sol = "I like monosodium monochloride on my chips."
        local_path = "meta/config/path"
        fd, path = tempfile.mkstemp(".txt")
        with open(path, "w") as f:
            f.write(file_contents)

        db = hammer_config.HammerDatabase()
        base = hammer_config.load_config_from_string("""
food:
    condiment: "monosodium monochloride"
    dish: "chips"
""", is_yaml=True)
        meta = hammer_config.load_config_from_string("""
{
  "food.announcement": "<path>",
  "food.announcement_meta": ["transclude", "subst"]
}
""".replace("<path>", path), is_yaml=False, path=local_path)

        db.update_core([base, meta])

        # Trigger merge before cleanup
        self.assertEqual(db.get_setting("food.dish"), "chips")

        # Cleanup
        os.remove(path)

        self.assertEqual(db.get_setting("food.announcement"), file_contents_sol)

    def test_meta_as_array_1(self):
        """
        Test that meta attributes that are an array.
        """
        db = hammer_config.HammerDatabase()
        base = hammer_config.load_config_from_string("""
foo:
    bar:
        base_test: "local_path"
""", is_yaml=True, path="base/config/path")
        meta = hammer_config.load_config_from_string("""
{
  "foo.bar.meta_test": "${foo.bar.base_test}",
  "foo.bar.meta_test_meta": ["subst"]
}
""", is_yaml=False, path="meta/config/path")
        db.update_core([base, meta])
        self.assertEqual(db.get_setting("foo.bar.base_test"), "local_path")
        self.assertEqual(db.get_setting("foo.bar.meta_test"), "local_path")

    def test_meta_subst_and_prependlocal(self):
        """
        Test that meta attributes that are an array.
        """
        db = hammer_config.HammerDatabase()
        base = hammer_config.load_config_from_string("""
foo:
    bar:
        base_test: "local_path"
""", is_yaml=True, path="base/config/path")
        meta = hammer_config.load_config_from_string("""
{
  "foo.bar.meta_test": "${foo.bar.base_test}",
  "foo.bar.meta_test_meta": ["subst", "prependlocal"]
}
""", is_yaml=False, path="meta/config/path")
        db.update_core([base, meta])
        self.assertEqual(db.get_setting("foo.bar.base_test"), "local_path")
        self.assertEqual(db.get_setting("foo.bar.meta_test"), "meta/config/path/local_path")

    def test_meta_append_bad(self):
        """
        Test that the meta attribute "append" catches bad inputs.
        """
        base = hammer_config.load_config_from_string("""
foo:
    bar:
        adc: "yes"
        dac: "no"
        dsl: "scala"
""", is_yaml=True)
        meta = hammer_config.load_config_from_string("""
{
  "foo.bar.dsl": ["python"],
  "foo.bar.dsl_meta": "append",
  "foo.bar.dac": "current_weighted"
}
""", is_yaml=False)
        with self.assertRaises(ValueError):
            hammer_config.combine_configs([base, meta])

        meta = hammer_config.load_config_from_string("""
{
  "foo.bar.dsl": "c++",
  "foo.bar.dsl_meta": "append",
  "foo.bar.dac": "current_weighted"
}
""", is_yaml=False)
        with self.assertRaises(ValueError):
            hammer_config.combine_configs([base, meta])

    def test_isupper(self):
        self.assertTrue('FOO'.isupper())
        self.assertFalse('Foo'.isupper())

    def test_split(self):
        s = 'hello world'
        self.assertEqual(s.split(), ['hello', 'world'])
        # check that s.split fails when the separator is not a string
        with self.assertRaises(TypeError):
            s.split(2)

if __name__ == '__main__':
    unittest.main()
