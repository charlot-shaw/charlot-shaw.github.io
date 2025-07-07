+++
title = "Making an Insomnia Reminder timer in NixOS and home-manager"
date = "2025-06-15"
[taxonomies]
tags = ["programming", "nixos", "nix", "projects", "home-manager", "systemd"]
+++

I have always struggled with insomnia, and lately Factorio: Space Age has made the midnight hours fly past way too quickly. 
Looking up and finding it's 3 AM already has happened too many times, so I figured that making a little script to remind me to sign out would be a neat little project.
Now it's going to be a write-up, tracing through how I solved the problem and pointing out some dead-ends along the way.

<!-- more -->


## Goal

Every ten minutes or so past say, 11 PM, I want a notification to pop up to remind me to consider if it's worth continuing what I'm doing versus going to bed.
I want it to include the actual time, and ideally show over a full-screened game.

## Ingredients

* [NixOS](https://nixos.org/), my operating system of choice.
* [Snowfall-lib](https://snowfall.org/), an opinionated framework for NixOS configuration. (Optional, it's opinionated afterall.)
* [home-manager](https://nix-community.github.io/home-manager/index.xhtml), used for configuring user-scoped packages and options under NixOS.
* [libnotify](https://gnome.pages.gitlab.gnome.org/libnotify/), specifically for it's cross-desktop CLI interface, `notify-send`
* [systemd](https://systemd.io/), the backbone of NixOS we'll be tapping into.



## Home-manager setup.

Using snowfall-lib, my NixOS configuration follows a specific structure. To make an enableable piece of config, a [module](https://snowfall.org/guides/lib/modules/) in snowfall terms, I make a file at `modules/home/util/insomnia_reminder/default.nix`

Remember to `git add` it so that Nix can see it.

With the standard boilerplate established in my repository, I soon have the following.

```nix
{
  pkgs,
  namespace,
  config,
  lib,
  ...
}: let
  cfg = config.${namespace}.util.logout_reminder;
in {
  options.${namespace}.util.logout_reminder = {
    enable = lib.mkEnableOption "Reminder to sign out in the evening.";
  };

  config = lib.mkIf cfg.enable {
    home.packages = [
      pkgs.libnotify
    ];
  };
}
```

(I'm going to leave explanations of the Nix parts out of the main post here, but if you need them, they'll be in spoilers)

<details>
<summary>
Walkthrough for those unfamiliar with Nix
</summary>

---

Starting at the top, let's work our way down.

```nix
{
  pkgs,
  namespace,
  config,
  lib,
  ...
}:
```

This is more or less the 'imports' for our configuration.

`pkgs` is the provided instance of `nixpkgs`, which lets us reference programs. How that works is out of scope for this post, so I'll leave it at that.

`namespace` is maybe unnecesary, but it's a variable containing the namespace of our module. If you copy-pasted my code into your snowfall-powered template, it would become the namespace you declared [here](https://snowfall.org/guides/lib/quickstart/#configure-snowfall-lib), and thus the module would look like one of your own modules. I disliked hardcoding a path, so I've used it this way.

`config` is the evaluated configuration. Simplifying here, Nix looks through your repository in multiple passes; the first time it gathers all the options you and your dependencies declare, the second time it finds out what you asked for those options to be set to, and then on a third pass it carries out the setup according to the information in the first two passes.

`...` Technically, this file is defining a function in nix, and this essentially means 'accept any number of additional arguments, but don't provide local aliases for them. If we need them, we'll use their unaliased names.'

Moving on, let's handle the next chunk. This is more or less the 'control panel' for the module.

```nix
let
  cfg = config.${namespace}.util.logout_reminder;
in {
  options.${namespace}.util.logout_reminder = {
    enable = lib.mkEnableOption "Reminder to sign out in the evening.";
  };
```

`let` here declares some local variables, scoped to the section following `in`.

`cfg = config.${namespace}.util.logout_reminder;` looks up a certain path in the evaluated configuration to find the config of this module, so we can reference it with a short name. Note how we're using `${namespace}` here, that's how the path in the configuration uses the `namespace` variable to stay be dynamic. `${some nix code}` is how you embed a Nix epression into configuration, including in this case, Nix code itself. 


`options.${namespace}.util.logout_reminder` establishes the path we looked up in the prior line. (Sorry it's kinda topsy turvy, but that's how nix rolls.) Starting with `options` means we're declaring how we can be configured in this block. (technically, it's an attribute set. Think dictionary in Python or map in Clojure.) 


`enable = lib.mkEnableOption "Reminder to sign out in the evening.";` This calls a function to set up a standard enable/disable toggle, with a doc-string.


Finally, we've got this section. `options` defined what could be configured, `config` is the implementation of those options.

```nix
  { # Ignore this, making the code highlighter happy with the fragment of code.
    config = lib.mkIf cfg.enable {
    home.packages = [
      pkgs.libnotify
    ];
  };
}
```

`lib.mkIf cfg.enable {}`, `cfg` is that shorthand path to our own config we established with the `let`. `lib.mkIf` will only include the next map if expression `cfg.enable` is true. IE, if we don't enable our configuration, our module becomes a no-op, with no effect on the system.

Note, we could look up any other piece of confiration if we needed to by just finding other paths in `config`. Handy if our module would conflict with something, or we needed to perform different logic if a different Desktop Environment or audio driver was installed.

`home.packages` is the list of packages installed for this user. `pkgs.libnotify` is the package containing `notify-send`. 

Hope this helps.

---
</details>

After enablabling the module like this:
```nix
# /homes/x86_64/sparrows@bough/default.nix

#birb is my namespace, btw
{birb.util.logout_reminder.enable = true;}
```

We will now have `libnotify` installed on our system, and are ready to figure out how to use it.


## Showing a notification

Let's see what `notify-send` has to offer.

```bash
Usage:
  notify-send [OPTION…] <SUMMARY> [BODY] - create a notification

Help Options:
  -?, --help                        Show help options

Application Options:
  -u, --urgency=LEVEL               Specifies the urgency level (low, normal, critical).
  -t, --expire-time=TIME            Specifies the timeout in milliseconds at which to expire the notification.
  -a, --app-name=APP_NAME           Specifies the app name for the notification
  -i, --icon=ICON                   Specifies an icon filename or stock icon to display.
  -c, --category=TYPE[,TYPE...]     Specifies the notification category.
  -e, --transient                   Create a transient notification
  -h, --hint=TYPE:NAME:VALUE        Specifies basic extra data to pass. Valid types are boolean, int, double, string, byte and variant.
  -p, --print-id                    Print the notification ID.
  -r, --replace-id=REPLACE_ID       The ID of the notification to replace.
  -w, --wait                        Wait for the notification to be closed before exiting.
  -A, --action=[NAME=]Text...       Specifies the actions to display to the user. Implies --wait to wait for user input. May be set multiple times. The name of the action is output to stdout. If NAME is not specified, the numerical index of the option is used (starting with 0).
  -v, --version                     Version of the package.
```

Looking over this, and some experimentation, we want `--expire-time`, so that we can make it stick around long enough to require us acknowledge it by dismissing it.
 30 seconds is 30,000 milliseconds, and plenty of time to be noticed.
For kicks, I added `--category reminder`. It's not part of [the standard](https://galago-project.org/specs/notification/0.9/x211.html), but might as well use what I searched up.

We don't have a specific icon, and `--urgency` might be relevant if the notification shows up in a more eye-catching way, but for now our invocation is this.
 
```bash
notify-send -t 30000 -c reminder "Consider signing out for the night." "It's getting late."
```

I want to see the present time in the notification though, so I have to actually acknowledge the hour.

`date +"%H:%M"` returns the current time in a 24 hour clock. A little bit of bash substitution, and we have `"A string with the present time of $(date +"%H:%M") embedded in it"`

```bash
notify-send -t 30000 -c reminder "Consider signing out for the night." "It's presently $(date +"%H:%M"), should you go to sleep?"
```

<div class="block">
<picture class="image" alt="A notification reading 'Consider signing out for the night: It's presently 23:17, should you go to sleep?">
<source srcset="notification.png" media="(orientation:landscape)">
<img src="notification_tight_crop.png">
</picture>
</div>

Oh, would you look at the time... Anyway, moving on, let's tackle systemd.

## The Scheduler

`cron` and the `crontab` file is not considered idiomatic in NixOS, so it was time to learn some systemd.

First, a significant footgun. home-manager and NixOS both have ways to configure systemd, but they are **different ways**. NixOS has an interface that's in more idiomatic Nix, but home-manager more closely follows the underlying systemd configuration. If you are looking up examples online, keep in mind whether it's a snippet of NixOS or home-manager configuration. To be clear, we're using the home-manager style here.

home-manager's documentation on systemd timers is [here](https://nix-community.github.io/home-manager/options.xhtml#opt-systemd.user.timers), and it's... sparse. It pretty much refers you to systemd's own documentation, which is extensive.

The sections I found the most use are [timers](https://www.freedesktop.org/software/systemd/man/latest/systemd.timer.html), [Units](https://www.freedesktop.org/software/systemd/man/latest/systemd.unit.html), and [Service](https://www.freedesktop.org/software/systemd/man/latest/systemd.service.html).

I'll walk you through the end result.


```nix
{
    # Because I didn't want a typo to be an issue, I made the service name a variable in the let block.
    systemd.user.timers.${unit_name} = {
      Unit = {
        Description = "timer for logout reminder to combat insomnia";
      };

      Install = {
        WantedBy = ["timers.target"];
      };

      Timer = {
        OnCalendar = "*-*-* 00,01,02,03,04,05,23:10,20,30,40,50,00";
        Unit = "${unit_name}.service";
      };
    };
}
```

`Unit` is the section to describe the... well, the unit itself. systemd uses Units as it's fundamental unit, naming things is hard. Anyway, consider it the metadata block for what we're trying to do.

`Install` is the section that determines what happens when systemd attempts to parse and integrate the unit into the system. In this case, we say that `timers.target` wants this unit.
 So, when systemd starts the `timers.target` unit, it will then start our timer unit. 
However, if for whatever reason, our timer crashes or has an issue during startup, `timers.target` won't be affected. 
If you have something mission-critical, maybe this is not what you want, but for the current project I'd rather not start failing parts of the operating system if we make a mistake.

`Timer` is the section that details the 'when', now we've declared the 'who' and 'why'.
`OnCalendar` means we want a wall-clock time, as opposed to something like the time since boot, and takes a cron configuration string.
You can check the validity of the string with `systemd-analyze calendar "your string here"`

This one probably could be restated more elegantly, but I didn't find a nice way to use the range operator while crossing midnight. Spoken aloud, it's something to the effect of:

> Trigger every day, every ten minutes between 11 PM and 5 AM.

`Unit` is the unit we wish to start when our timer fires.

```nix
{
    systemd.user.services.${unit_name} = {
      Unit = {
        Description = "logout reminder to combat insomnia";
      };

      Service = {
        Type = "oneshot";
        ExecStart = "${pkgs.writeShellScript "${unit_name}" ''
          ${pkgs.libnotify}/bin/notify-send -t 30000 -c reminder "Consider signing out for the night." "It's presently $(date +"%H:%M"), should you go to sleep?"
        ''}";
      };
    };
}
```

In the same style, we have a `Unit` section, and then the new part, the `Service` section. `Type = "oneshot";` says that systemd just needs to run the process, it doesn't need to monitor it further than that. Said another way, our command isn't some long running service that systemd should restart if it fails.

`ExecStart = "${pkgs.writeShellScript "${unit_name}" '' foo ''` invokes a function in `pkgs` called `writeShellScript`. This is an interesting one, because most functions in `pkgs` evaluate out to programs more or less. This one evaluates out to a shell script with the contents 'foo' instead of a program defined elsewhere.

`${pkgs.libnotify}/bin/notify-send` Looks up `libnotify` in `pkgs`, and calls it. As we aren't digging deeper into it, it returns the path in the Nix store where `libnotify` is installed, to which we append `/bin/notify-send`. This essentially lets us hard-code the path to the `notify-send` executable, instead of needing to look it up in `$PATH`. We know it will be in the Nix store because we installed it.

There's nothing wrong with using `$PATH` and we have used it for `date`, but we can be more precise and avoid any situations where `$PATH` and `pkgs` could disagree.
Unlikely, but this code is somewhat generic, so being rigorous is worth it.

## All Together Now

After doing all the prior steps, I realized my solutions was rather verbose, and started trimming declarations out of it and seeing what would be handled by default. I left them in the description as being more explicit in a tutorial isn't a bad thing. That said, here's the skeletonized version.

```nix
{
  pkgs,
  namespace,
  config,
  lib,
  ...
}: let
  cfg = config.${namespace}.util.logout_reminder;
  unit_name = "insomnia-reminder";
in {
  options.${namespace}.util.logout_reminder = {
    enable = lib.mkEnableOption "Reminder to sign out in the evening.";
  };

  config = lib.mkIf cfg.enable {
    home.packages = [
      pkgs.libnotify
    ];

    systemd.user.timers.${unit_name} = {
      Timer = {
        OnCalendar = "*-*-* 00,01,02,03,04,05,23:10,20,30,40,50,00";
      };
    };

    systemd.user.services.${unit_name} = {
      Unit = {
        Description = "logout reminder to combat insomnia";
      };

      Service = {
        Type = "oneshot";
        ExecStart = "${pkgs.writeShellScript "${unit_name}" ''
          ${pkgs.libnotify}/bin/notify-send -t 30000 -c reminder "Consider signing out for the night." "It's presently $(date +"%H:%M"), should you go to sleep?"
        ''}";
      };
    };
  };
}
```


## Troubleshooting
Here's some tricks I figured out in case you're doing similar things and aren't getting the expected results.

### systemctl doesn't show my service/timer?

systemd keeps the user's services hidden under a flag. 

`systemctl status insomnia-reminder.service` won't find our service, but `systemctl --user status insomnia-reminder.service` will.

### How can I see my running timers?

`systemctl --user list-timers` will do the trick.

### Where does home-manager put your service definitions?

They're in `~/.config/systemd/user`. Opening them up with a text editor is a great way to double-check Nix and home-manager are doing what you think they're doing.
It also lets you find the path for our generated shell script, so we can run that and see if it's working.

### I want to use this to start GUI programs!

That's nifty, but unfortunately, systemd user services are intended to be per-user, not per-session. The Arch Wiki has more information [here](https://wiki.archlinux.org/title/Systemd/User). 

## Further Exercises

Some ideas I had that I might pursue, but written up as exercises for the reader. Mostly pitched to people learning Nix, cause I don't know too much to add on the systemd side.

* Expand the options for configuration. Can you allow the user to change the title of the notification in the same piece of configuration where they enable the module?
  * What about the body? Can you allow configuration while keeping dynamic content like the current time?
  * What if the user doesn't want to use 24 hour time?
* `notify-send` has that `--action` option that adds clickable buttons to the notification. Add one that trigggers the screensaver when clicked.
* Is there a more elegant cron config that's not got so much manual enumeration?

## Thanks For Reading

I hope this helped you in your own experiments in configuring NixOS or systemd for your own personal needs.
For now though, I hear [Gleba](https://wiki.factorio.com/Gleba) is beautiful this time of night.
