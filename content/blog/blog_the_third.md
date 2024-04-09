+++
title = "Blog The Third: Nix and Zola"
date = "2024-04-02"
draft = true
[taxonomies]
tags = ["nix", "web", "meta", "zola", "github", "programming", "clojure"]
+++

Repeatedly switching Site Generators is something that happens to everyone, right?
<!-- more -->
## Thawing Out

I'd initially really enjoyed Cryogen, and mucked about for a while under the hood. However, it felt overbearing, hard to modify in it's thoroughness. 
I wanted something lighter, something I could really dig into. For a while that was [Quickblog](https://github.com/borkdude/quickblog).
Another month or so was spent mucking about in there, but I grew to dislike how opinionated the codebase was. 
With no disrespect to Borkdude intended, it felt like a very personal project.

And then I realized. The whole point of having a blogging engine, was to actually write blog posts. 
With my focus on tweaking things, and building my own version of everything, I'd stopped writing, and gotten stuck down a rabbithole of implementation.

## A New Kind Of Snowflake

I also had started getting into [Nix](https://nixos.org/), and been using it more and more.
First a laptop, then my server, then my desktop all got nixified and flaked.

The ability to tweak things and get them wrong without giving up on a readily avialible working system intrigued me,
and the knowledge I was gaining was great.
It's filtered through Nix's lense, but I have learned more about Linux, systemd, and how to compose a system than I had before, simply from being able to poke stuff.

There's so much to learn still, but I wanted to start putting things to use, outside of managing dev environments and my systems.
I wanted to learn Nix as a build tool.z

A static site generator and my blog seemed a great place to start.

## ...Zola?

Not wanting to be sucked down into the generator's details, I opted for [Zola](https://getzola.org).
It's in [Rust](https://rustlang.org), a language I want to learn down the road, and it met all my requirements for the blog.

With a basic flake in place, I migrated my content over to Zola's preferred setup.
I picked out a theme, and started to work on Github Actions for my CI.

Only there was a hitch. Zola wanted to use submodules, and I could not get Nix to cooperate on them.

I mucked around for a while, and then in the name of having a working blog, opted to make a *very* quick theme using [Bulma](https://bulma.io).
The skeleton of [Anemone Theme](https://anemone.pages.dev/) was stripped and cleaned to guide my own work, and over the course of an evening, things fell into place.
