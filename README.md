# Files for Routing Video

These files are the code shown in the [YouTube video about Fulcro routing](https://youtu.be/HJBI24yAdBQ).

The samples are coded in the following files:

- `src/main/routing/ui/components.cljs` : Most of the UI
- `src/main/routing/ui/root.cljs` : The final HTML5 top-level routing code
- `src/main/routing/html5_routing.cljs` : The final HTML5 routing tree, loading code,
and pushy/bidi integration.
- `src/cards/routing/intro.cljs` : The devcards running the simple routing examples.

## Setting Up

The shadow-cljs compiler uses all cljsjs and NPM js dependencies through
NPM. If you use a library that is in cljsjs you will also have to add
it to your `package.json`.

You also cannot compile this project until you install the ones it
depends on already:

```
$ npm install
```

or if you prefer `yarn`:

```
$ yarn install
```

Adding NPM Javascript libraries is as simple as adding them to your
`package.json` file and requiring them! See the
[the Shadow-cljs User's Guide](https://shadow-cljs.github.io/docs/UsersGuide.html#_javascript)
for more information.

## Development Mode

Shadow-cljs handles the client-side development build. The file
`src/main/routing/client.cljs` contains the code to start and refresh
the client for hot code reload.

Running all client development builds:

```
$ npx shadow-cljs watch main cards test
...
shadow-cljs - HTTP server for ":main" available at http://localhost:8020
shadow-cljs - HTTP server for ":test" available at http://localhost:8022
shadow-cljs - HTTP server for ":cards" available at http://localhost:8023
...
```

The compiler will detect which builds are affected by a change and will minimize
incremental build time.

NOTE: The server wil start a web server for all three builds (on different ports).
You typically do not need the one for main because you'll be running your
own server, but it is there in case you are only going to be writing
a client-side app that has no server API.

The URLs for working with cards and tests are:

- Cards: [http://localhost:8023/cards.html](http://localhost:8023/cards.html)
- Tests: [http://localhost:8022/index.html](http://localhost:8022/index.html)
- Main: [http://localhost:8020/index.html](http://localhost:8020/index.html) (NO API SERVER)

See the server section below for working on the full-stack app itself.

### Client REPL

The shadow-cljs compiler starts an nREPL. It is configured to start on
port 9000 (in `shadow-cljs.edn`).

In IntelliJ, simply add a *remote* Clojure REPL configuration with
host `localhost` and port `9000`.

If you're using CIDER
see [the Shadow-cljs User's Guide](https://shadow-cljs.github.io/docs/UsersGuide.html#_cider)
for more information.

### Preloads

There is a preload file that is used on the development build of the
application `routing.development-preload`. You can add code here that
you want to execute before the application initializes in development
mode.

### Fulcro Inspect

The Fulcro inspect will preload on the development build of the main
application and cards. You can activate it by pressing CTRL-F while in
the application. If you need a different keyboard shortcut (e.g. for
Windows) see the docs on github.
