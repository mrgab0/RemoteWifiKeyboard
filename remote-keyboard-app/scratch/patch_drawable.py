import re
file_path = 'app/src/main/java/com/remotekeyboard/RemoteInputMethodService.java'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Replace onCreateInputView up to layout.setGravity
old_on_create = '''    public View onCreateInputView() {
        android.widget.FrameLayout rootFrame = new android.widget.FrameLayout(this);
        AnimatedGradientView animatedBg = new AnimatedGradientView(this);
        rootFrame.addView(animatedBg, new android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, 
                android.view.ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout layout = new LinearLayout(this);
        rootFrame.addView(layout, new android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, 
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        layout.setPadding(16, 16, 16, 16);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);'''

new_on_create = '''    public View onCreateInputView() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackground(new AnimatedGradientDrawable());
        layout.setPadding(16, 16, 16, 16);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);'''

content = content.replace(old_on_create, new_on_create)

# 2. Replace return rootFrame; with return layout;
content = content.replace('applyTheme(layout);\n\n        return rootFrame;', 'applyTheme(layout);\n\n        return layout;')

# 3. Replace AnimatedGradientView class with AnimatedGradientDrawable
old_class_pattern = r'class AnimatedGradientView extends android\.view\.View \{.*?(?=\}\s*$)\}'
# wait, regex with DOTALL might be tricky if it matches too much.
# Let's just find "class AnimatedGradientView" and replace it until the end of file (since it's at the end)

old_class_start = 'class AnimatedGradientView extends android.view.View {'

new_class = '''class AnimatedGradientDrawable extends android.graphics.drawable.Drawable {
        private android.graphics.Paint paint;
        private float offset = 0;
        private android.graphics.LinearGradient gradient;
        private android.graphics.Matrix matrix;

        public AnimatedGradientDrawable() {
            paint = new android.graphics.Paint();
            matrix = new android.graphics.Matrix();
            int[] colors = {
                android.graphics.Color.parseColor("#ff007f"), 
                android.graphics.Color.parseColor("#7400b8"), 
                android.graphics.Color.parseColor("#00f5d4"), 
                android.graphics.Color.parseColor("#ff007f")  
            };
            gradient = new android.graphics.LinearGradient(0, 0, 1000, 1000, colors, null, android.graphics.Shader.TileMode.MIRROR);
            paint.setShader(gradient);

            android.animation.ValueAnimator animator = android.animation.ValueAnimator.ofFloat(0, 2000);
            animator.setDuration(4000);
            animator.setRepeatCount(android.animation.ValueAnimator.INFINITE);
            animator.setInterpolator(new android.view.animation.LinearInterpolator());
            animator.addUpdateListener(anim -> {
                offset = (float) anim.getAnimatedValue();
                matrix.setTranslate(offset, offset);
                gradient.setLocalMatrix(matrix);
                invalidateSelf();
            });
            animator.start();
        }

        @Override
        public void draw(android.graphics.Canvas canvas) {
            canvas.drawRect(getBounds(), paint);
        }

        @Override
        public void setAlpha(int alpha) { paint.setAlpha(alpha); }

        @Override
        public void setColorFilter(android.graphics.ColorFilter colorFilter) { paint.setColorFilter(colorFilter); }

        @Override
        public int getOpacity() { return android.graphics.PixelFormat.TRANSLUCENT; }
    }'''

idx = content.find(old_class_start)
if idx != -1:
    # Remove everything from idx to the second to last brace
    # Actually just string replace the whole block if I know exactly what it is.
    content = content[:idx] + new_class + '\n}\n'
else:
    print('Could not find AnimatedGradientView')

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
print('Patched drawable successfully')
