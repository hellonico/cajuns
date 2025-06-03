(ns parallax.e01)

(defn parallax-style []
 [:style
  "
  /* The scroll container is 100vh tall */
  .parallax-container {
    height: 100vh;
    overflow-x: hidden;
    overflow-y: auto;
    perspective: 1px; /* small perspective makes the Z‐offset visible */
  }

  /* The parallax ‘stack’ must be taller than 100vh so you can scroll */
  .parallax {
    position: relative;
    height: 200vh; /* <-- tall enough to scroll */
    transform-style: preserve-3d;
  }

  /* Every layer sits absolutely at top:0, left:0, but only fills 100vh */
  .parallax-layer {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100vh;
  }

  /* BACKGROUND: pulled back in Z‐space, then scaled up to fill */
  .background {
    transform: translateZ(-1px) scale(2);
    background: url('https://images6.alphacoders.com/428/428645.jpg') no-repeat center center;
    background-size: cover;
    z-index: -1; /* sit behind the foreground text */
  }

  /* FOREGROUND: sits at Z=0, full 100vh, centered text */
  .foreground {
    transform: translateZ(0) scale(1);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 3rem;
    color: white;
  }

  /* OPTIONAL: some extra content after the “hero” so you can scroll past */
  .after-content {
    position: relative;
    margin-top: 100vh; /* push it below the hero */
    padding: 2rem;
    background: #fff;
    font-size: 1.5rem;
    color: #333;
  }
  "])

(defn parallax-component []
 [:div.parallax-container
  [parallax-style]
  [:div.parallax
   [:div.parallax-layer.background]
   [:div.parallax-layer.foreground "Parallax Effect!"]
   ;; This extra div sits below the 100vh hero so you can scroll into it
   [:div.after-content
    "Here’s some normal content you can scroll into once the hero passes."]]])
