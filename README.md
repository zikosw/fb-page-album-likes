## Screenshot
![example](/screenshot.jpg?raw=true)
### Development mode

This project require Java and [Leiningen](https://leiningen.org/) for automating Clojure projects without setting your hair on fire.

To install lein follow step below.
1. Download the lein script.
2. Move script to `$PATH` your shell can execute it (eg. `~/bin`)
3. Make script executable.
    ```
    chmod +x ~/bin/lein
    ```
4. Enjoy!

To start the Figwheel compiler, navigate to the project folder and run the following command in the terminal:

```
lein figwheel
```

Figwheel will automatically push cljs changes to the browser.
Once Figwheel starts up, you should be able to open the `public/index.html` page in the browser.


### Building for production

```
yarn build
yarn deploy
```

```
lein clean
lein package
```
