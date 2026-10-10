import re

file_path = "app/src/main/java/com/remotekeyboard/RemoteInputMethodService.java"

with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

# 1. Add imports
imports_to_add = """
import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
"""
# Insert after the first import
content = re.sub(r'(import [^;]+;)', r'\1' + imports_to_add, content, count=1)

# 2. Replace onCreateInputView
old_on_create_loose = r'@Override\s*public View onCreateInputView\(\)\s*\{[\s\S]*?return keyboardContainer;\s*\}'
new_on_create = '''    @Override
    public View onCreateInputView() {
        FrameLayout rootLayout = new FrameLayout(this);
        rootLayout.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        ));

        AnimatedGradientView bgView = new AnimatedGradientView(this);
        rootLayout.addView(bgView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        keyboardContainer = new LinearLayout(this);
        keyboardContainer.setOrientation(LinearLayout.VERTICAL);
        keyboardContainer.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        ));
        keyboardContainer.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        
        rootLayout.addView(keyboardContainer);

        buildVisualKeyboard("default");
        return rootLayout;
    }'''

if re.search(old_on_create_loose, content):
    content = re.sub(old_on_create_loose, new_on_create, content)
    print("Found and replaced onCreateInputView.")
else:
    print("Could not find onCreateInputView.")

# 3. Modify theme array in buildVisualKeyboard
theme_pattern = r'int\[\] theme = themes\.getOrDefault\(themeId, themes\.get\("default"\)\);'
new_theme = '''int[] theme = themes.getOrDefault(themeId, themes.get("default"));
        // PRO MODE OVERRIDES
        theme[0] = android.graphics.Color.WHITE; // Text color
        theme[1] = android.graphics.Color.parseColor("#121212"); // Normal key background
        theme[2] = android.graphics.Color.TRANSPARENT; // Pressed key background (reveals gradient)
        theme[3] = android.graphics.Color.WHITE; // Text color pressed'''

content = content.replace('int[] theme = themes.getOrDefault(themeId, themes.get("default"));', new_theme)

# 4. Add AnimatedGradientView inner class
inner_class = '''

    class AnimatedGradientView extends android.view.View {
        private Paint paint;
        private float offset = 0;
        private LinearGradient gradient;
        private Matrix matrix;

        public AnimatedGradientView(android.content.Context context) {
            super(context);
            paint = new Paint();
            matrix = new Matrix();
            int[] colors = {
                android.graphics.Color.parseColor("#ff007f"), 
                android.graphics.Color.parseColor("#7400b8"), 
                android.graphics.Color.parseColor("#00f5d4"), 
                android.graphics.Color.parseColor("#ff007f")  
            };
            gradient = new LinearGradient(0, 0, 1000, 1000, colors, null, Shader.TileMode.MIRROR);
            paint.setShader(gradient);

            ValueAnimator animator = ValueAnimator.ofFloat(0, 2000);
            animator.setDuration(4000);
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.setInterpolator(new LinearInterpolator());
            animator.addUpdateListener(anim -> {
                offset = (float) anim.getAnimatedValue();
                matrix.setTranslate(offset, offset);
                gradient.setLocalMatrix(matrix);
                invalidate();
            });
            animator.start();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
        }
    }
}'''

# Insert inner class right before the final closing brace of RemoteInputMethodService
content = content.rstrip()
if content.endswith('}'):
    content = content[:-1] + inner_class
else:
    print("Could not find closing brace of class")

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)

print("Patch applied successfully.")
