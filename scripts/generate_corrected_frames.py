#!/usr/bin/env python3
"""
Glyph Toy Eyes Animation Frame Generator - Fixed Version
Corrects eye shape and connection area issues
"""

import math
import csv

def create_eye_frame(left_pupil_offset_x=0, right_pupil_offset_x=0, left_pupil_offset_y=0, right_pupil_offset_y=0):
    """
    Create a 25x25 frame with eyes and pupils at specified offsets
    Fixed version with correct eye shape and no connection between eyes
    """
    # Initialize 25x25 matrix with zeros
    frame = [[0 for _ in range(25)] for _ in range(25)]
    
    # Eye parameters from GlyphEyesService.kt - adjusted for better shape
    eye_center_left = (8, 12)
    eye_center_right = (17, 12)
    eye_radius_x = 3.5  # Slightly smaller horizontal radius
    eye_radius_y = 5.5  # Slightly smaller vertical radius
    pupil_radius = 1.8  # Slightly smaller pupil
    
    # Calculate actual pupil positions
    left_pupil_center = (eye_center_left[0] + left_pupil_offset_x, eye_center_left[1] + left_pupil_offset_y)
    right_pupil_center = (eye_center_right[0] + right_pupil_offset_x, eye_center_right[1] + right_pupil_offset_y)
    
    # Helper function to check if point is inside ellipse with better precision
    def is_inside_ellipse(x, y, center_x, center_y, radius_x, radius_y):
        dx = x - center_x
        dy = y - center_y
        # Use stricter ellipse calculation
        return (dx * dx) / (radius_x * radius_x) + (dy * dy) / (radius_y * radius_y) < 0.95
    
    # Helper function to check if point is inside circle
    def is_inside_circle(x, y, center_x, center_y, radius):
        dx = x - center_x
        dy = y - center_y
        return dx * dx + dy * dy <= radius * radius
    
    # Fill the frame
    for y in range(25):
        for x in range(25):
            pixel_value = 0
            
            # Check if pixel is inside left eye (white part)
            if is_inside_ellipse(x, y, eye_center_left[0], eye_center_left[1], eye_radius_x, eye_radius_y):
                pixel_value = 2040  # White eye
                
                # Check if pixel is inside left pupil (black part)
                if is_inside_circle(x, y, left_pupil_center[0], left_pupil_center[1], pupil_radius):
                    pixel_value = 0  # Black pupil
            
            # Check if pixel is inside right eye (white part)
            elif is_inside_ellipse(x, y, eye_center_right[0], eye_center_right[1], eye_radius_x, eye_radius_y):
                pixel_value = 2040  # White eye
                
                # Check if pixel is inside right pupil (black part)
                if is_inside_circle(x, y, right_pupil_center[0], right_pupil_center[1], pupil_radius):
                    pixel_value = 0  # Black pupil
            
            # NO CONNECTION between eyes - remove this part completely
            # The area between eyes should remain black (0)
            
            frame[y][x] = pixel_value
    
    return frame

def frame_to_single_row(frame):
    """Convert 25x25 frame to single row (625 values)"""
    values = []
    for row in frame:
        for value in row:
            values.append(value)
    return values

def create_visual_preview(frame):
    """Create a visual representation of the frame"""
    visual = []
    for row in frame:
        line = ""
        for val in row:
            if val == 0:
                line += "."
            else:
                line += "O"
        visual.append(line)
    return visual

def main():
    """Generate corrected animation frames and save as CSV"""
    print("Generating CORRECTED Glyph Toy Eyes Animation Frames...")
    
    # Frame 1: Pupils looking right (2 pixels)
    frame1 = create_eye_frame(left_pupil_offset_x=2, right_pupil_offset_x=2)
    
    # Frame 2: Pupils center
    frame2 = create_eye_frame(left_pupil_offset_x=0, right_pupil_offset_x=0)
    
    # Frame 3: Pupils looking left (2 pixels)
    frame3 = create_eye_frame(left_pupil_offset_x=-2, right_pupil_offset_x=-2)
    
    # Frame 4: Pupils center again
    frame4 = create_eye_frame(left_pupil_offset_x=0, right_pupil_offset_x=0)
    
    frames = [frame1, frame2, frame3, frame4]
    frame_names = ["Frame1_Looking_Right", "Frame2_Center", "Frame3_Looking_Left", "Frame4_Center"]
    
    # Show visual preview of first frame
    print("\n=== VISUAL PREVIEW (Frame 1 - Looking Right) ===")
    visual = create_visual_preview(frame1)
    for line in visual:
        print(line)
    
    # Save corrected simple format CSV
    corrected_filename = "glyph_eyes_animation_corrected.csv"
    with open(corrected_filename, 'w', newline='') as csvfile:
        writer = csv.writer(csvfile)
        for frame in frames:
            single_row = frame_to_single_row(frame)
            writer.writerow(single_row)
    
    print(f"\n✅ Saved corrected version: {corrected_filename}")
    
    # Also save individual corrected frames for comparison
    for i, (frame, name) in enumerate(zip(frames, frame_names)):
        filename = f"glyph_eyes_corrected_{name.lower()}.csv"
        with open(filename, 'w', newline='') as csvfile:
            writer = csv.writer(csvfile)
            writer.writerow([f"Corrected Glyph Eyes - {name}"])
            writer.writerow(["Fixed eye shape and removed connection between eyes"])
            writer.writerow([])
            single_row = frame_to_single_row(frame)
            writer.writerow(single_row)
        print(f"✅ Saved: {filename}")
    
    print("\n🔧 CORRECTIONS MADE:")
    print("   - Fixed eye shape (more precise ellipse)")
    print("   - Removed white connection between eyes")
    print("   - Eliminated protruding white dots")
    print("   - Adjusted pupil size for better proportion")

if __name__ == "__main__":
    main()