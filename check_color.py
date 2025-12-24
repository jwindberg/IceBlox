from PIL import Image

img = Image.open('app/src/main/res/drawable/iceblox.gif')
sprite = img.crop((0, 0, 30, 30))
sprite = sprite.convert("RGBA")

# Get corners
corners = [
    sprite.getpixel((0, 0)),
    sprite.getpixel((29, 0)),
    sprite.getpixel((0, 29)),
    sprite.getpixel((29, 29))
]

print(f"Corner pixels (RGBA): {corners}")

# Check if alpha is 0 (Transparent) or 255 (Opaque)
# And if opaque, is it White (255, 255, 255)?
